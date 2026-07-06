---
name: reference_backend_jdk21
description: Para correr mvn verify del backend hay que fijar JAVA_HOME al JDK 21 (el shell por defecto usa 17/25)
type: reference
---

El backend de parking requiere **Java 21**, pero el shell por defecto de esta máquina Windows tiene JAVA_HOME en JDK 17 (Eclipse Adoptium) y `java` en PATH resuelve a 25 → `mvn` falla con "release version 21 not supported".

**Para verificar el backend** (`mvn clean verify`), exportar primero:
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
```
JDKs disponibles en `C:\Program Files\Java\`: jdk-17, jdk-21.0.11.10-hotspot, jdk-25. Usar el 21.

Además, cuidado con `mvn ... | tail`: el exit code del pipe es el de `tail`, no el de Maven — comprobar SIEMPRE `BUILD SUCCESS/FAILURE` en el output, no el exit code.
