package com.noober.asmplugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class ASMPlugin : Transform(), Plugin<Project> {
    override fun getName(): String = "asmTest"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> = TransformManager.CONTENT_CLASS

    override fun isIncremental(): Boolean = false

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = TransformManager.SCOPE_FULL_PROJECT

    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
        android.registerTransform(this)
    }

    override fun transform(transformInvocation: TransformInvocation) {
//        super.transform(transformInvocation)
        println("start transform ---------------------------")
        val inputs = transformInvocation.inputs
        val outputProvider: TransformOutputProvider? = transformInvocation.outputProvider
        outputProvider?.let {
            inputs.forEach { input ->
                input.directoryInputs.forEach {
                    handelDirectoryInput(it, outputProvider)
                }

                input.jarInputs.forEach {
                    println("jarInputsforEach:${it.name}")
                    handleJarInputs(it, outputProvider)
                }
            }
        }
        println("end transform ---------------------------")
    }


    private fun handelDirectoryInput(directoryInput: DirectoryInput, outputProvider: TransformOutputProvider){
        if(directoryInput.file.isDirectory){
            directoryInput.file.walk()
                .filter {
                    it.isFile && it.name.endsWith(".class") && !it.name.startsWith("R$")
                            && it.name != "R.class" && it.name != "BuildConfig.class"
                }
                .forEach {
                    println("name---forEach:${it.absolutePath}")
                    val classReader = ClassReader(it.readBytes())
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    val classVisitor = MyClassVisitor(classWriter)
                    classReader.accept(classVisitor, EXPAND_FRAMES)
                    val fos = FileOutputStream(it.absolutePath)
                    fos.write(classWriter.toByteArray())
                    fos.close()
                }
        }

        //处理完输入文件之后，要把输出给下一个任务
        val dest = outputProvider.getContentLocation(directoryInput.name,
        directoryInput.contentTypes, directoryInput.scopes,
        Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    private fun handleJarInputs(jarInput: JarInput, outputProvider: TransformOutputProvider){
        //判断是否为jar文件
        if (jarInput.file.absolutePath.endsWith(".jar")){
            var jarName = jarInput.name
            val newJarName = DigestUtils.md5Hex(jarInput.file.absolutePath)
            println("newJarName:${newJarName}")
            if(jarName.endsWith(".jar")){
               jarName = jarName.substring(0, jarName.length - 4)
            }
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            val tmpFile = File(jarInput.file.parent + File.separator + "classes_tmp.jar")
            //删除上次的缓存
            tmpFile.deleteOnExit()
            val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
//            jarInputsforEach:org.jetbrains.kotlin:kotlin-android-extensions-runtime:1.3.41
//            newJarName:7a452d78f33d6fdf3a920c486761855a
//            entryName:META-INF/
//            entryName:META-INF/MANIFEST.MF
//            entryName:META-INF/kotlin-android-extensions-runtime.kotlin_module
//            entryName:kotlinx/
//            entryName:kotlinx/android/
//            entryName:kotlinx/android/extensions/
//            entryName:kotlinx/android/extensions/ContainerOptions.class
//            对内容进行筛选，取出class文件
            while (enumeration.hasMoreElements()){
                val jarEntry = enumeration.nextElement()
                val entryName = jarEntry.name
                println("entryName:$entryName")
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(zipEntry)
                jarOutputStream.putNextEntry(zipEntry)

                //进行class插桩
                if(entryName.endsWith(".class") && !entryName.startsWith("R$")
                    && entryName != "R.class" && entryName != "BuildConfig.class"){
                    val classReader = ClassReader(IOUtils.toByteArray(inputStream))
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    val classVisitor = MyClassVisitor(classWriter)
                    classReader.accept(classVisitor, EXPAND_FRAMES)
                    jarOutputStream.write(classWriter.toByteArray())
                }else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }

            jarOutputStream.close()
            jarFile.close()

            //处理完输入文件之后，要把输出给下一个任务
            val dest = outputProvider.getContentLocation(jarName + newJarName,
                jarInput.contentTypes, jarInput.scopes, Format.JAR)
            println("dest:${dest.absolutePath}")
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }


}