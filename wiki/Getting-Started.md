# Getting Started

[Home](Home.md) · [Gameplay](Gameplay.md) · [Source and Status](Source-and-Status.md)

**Current baseline:** JDK 17+ and the repository checkout.

```sh
mkdir -p out
javac --release 17 -d out src/main/java/veil/*.java
java -cp out veil.MaleficVeil
```

Java2D launcher provides SOLO, HOST LAN and JOIN LAN paths. Experimental dedicated host:

```sh
java -cp out veil.LanServer 7777
```

Optional GPU environment preview is a separate Maven target and does **not** yet replace or fully connect to the shooter:

```sh
mvn -B -ntp -f gpu-client/pom.xml -DskipTests package
java -jar gpu-client/target/MaleficVeil-GPU.jar
```

Controls and available menu options differ between clients. See [README](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/README.md), [hosting](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/DEDICATED_HOSTING.md), [GPU status](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/GPU_CLIENT_IMPLEMENTATION.md), and [desktop updates](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/DESKTOP_RELEASE_AND_UPDATES.md). For build-specific truth, inspect the current commit and workflow results; old status documents can lag new changes.
