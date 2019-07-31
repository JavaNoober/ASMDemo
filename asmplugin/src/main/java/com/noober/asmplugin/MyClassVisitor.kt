package com.noober.asmplugin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MyClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM4, cv), Opcodes {

    private lateinit var className: String

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        println("MyClassVisitor:$name")
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        println("visitMethod:$name")
        if(name == "onCreate2"){
            return MyMethodVisitor(methodVisitor)
        }
        return methodVisitor//不能直接return super，否则会导致代码重复生成
    }
}