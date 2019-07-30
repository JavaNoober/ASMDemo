package com.noober.asmplugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile

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

    }


}