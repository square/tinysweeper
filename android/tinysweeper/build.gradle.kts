plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.protobuf)
  `maven-publish`
}

android {
  namespace = "com.squareup.tinysweeper"
  compileSdk = 34

  defaultConfig {
    minSdk = 24
    aarMetadata {
      minCompileSdk = 33
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  // Extra proto source files besides the ones residing under "src/main".
  protobuf(files("../../proto/squareup/tinysweeper/android"))
  protobuf(files("../../proto/squareup/lando/challenge"))

  implementation(libs.protobuf.java)
  implementation(libs.integrity)
  implementation(libs.play.services.base)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.retrofit)
  implementation(libs.converter.protobuf)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)
  implementation(libs.threetenabp)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.threetenbp)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}

protobuf {
  generateProtoTasks {
    all().forEach {
      it.builtins {
        create("java") {}
      }
    }
  }
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = "com.squareup.tinysweeper"
      artifactId = "tinysweeper"
      version = project.properties["ts.version"].toString()

      afterEvaluate {
        from(components["release"])
      }
    }
  }
  repositories {
    maven {
      url = uri(project.properties["mavenUrl"].toString())
      credentials(PasswordCredentials::class)
    }
  }
}

tasks.withType<PublishToMavenRepository> {
  // workaround for https://github.com/gradle/gradle/issues/22641
  val predicate =
    provider {
      println(
        "Publishing ${publication.groupId}:${publication.artifactId}:${publication.version} to ${repository.url}"
      )
    }
  doFirst { predicate.get() }
}
