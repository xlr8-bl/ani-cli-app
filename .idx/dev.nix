# Firebase Studio / Project IDX workspace config for XLR8 (native Android, Kotlin, Gradle).
# Docs: https://firebase.google.com/docs/studio/customize-workspace
{ pkgs, ... }: {
  # Nixpkgs channel.
  channel = "stable-24.05";

  # Packages available in the workspace terminal/build.
  packages = [
    pkgs.jdk17
    pkgs.unzip
    pkgs.git
  ];

  # Environment variables.
  env = {
    JAVA_HOME = "${pkgs.jdk17.home}";
    # Gradle wrapper caches here so rebuilds are faster.
    GRADLE_USER_HOME = "/home/user/.gradle";
  };

  idx = {
    # Editor extensions (Kotlin + Gradle tooling).
    extensions = [
      "fwcd.kotlin"
      "vscjava.vscode-gradle"
      "redhat.java"
    ];

    workspace = {
      # Run once when the workspace is first created.
      onCreate = {
        make-gradlew-executable = "chmod +x ./gradlew";
        # Warm the Gradle wrapper (downloads the distribution) so the first
        # emulator run is faster. Safe to fail if offline.
        prime-gradle = "./gradlew --version || true";
        default.openFiles = [
          "README.md"
          "app/src/main/java/com/xlr8/app/ui/home/HomeScreen.kt"
        ];
      };
      # Run every time the workspace starts.
      onStart = {
        make-gradlew-executable = "chmod +x ./gradlew";
      };
    };

    # Enable the Android emulator preview. Firebase Studio builds the debug APK
    # and installs it onto its managed emulator — no local SDK/emulator needed.
    previews = {
      enable = true;
      previews = {
        android = {
          manager = "android";
        };
      };
    };
  };
}
