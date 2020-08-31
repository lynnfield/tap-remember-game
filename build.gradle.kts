// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        // todo remove buildsrc fix when 4.2.0-alpha09
        classpath("com.android.tools.build:gradle:4.2.0-alpha08")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = java.net.URI("https://dl.bintray.com/arrow-kt/arrow-kt/") }
    }
}

val clean by tasks.registering(Delete::class) {
    delete(rootProject.buildDir)
}