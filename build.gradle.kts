// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
    }
}
