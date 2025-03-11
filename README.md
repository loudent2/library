# Spring Boot Gradle Template

This is a basic Spring Boot template using Gradle as the build tool. It serves as a starting point for new Java projects.

## 📌 Features
- Java 17
- Spring Boot 3.2.0
- Gradle for dependency management
- Basic Spring Boot application setup
- Pre-configured `.gitignore`
- Sample unit test

## 🚀 Getting Started

### **1. Clone the Repository**
```sh
git clone https://github.com/your-username/template-project.git
cd template-project
```

### 2. Rename the Project
If you are creating a new project from this template, update the following:

Modify settings.gradle:
```gradle
rootProject.name = 'your-project-name'
```

Modify build.gradle
```gradle
group = 'com.yourcompany'
```
#### Rename the Java Package
Update the package structure under src/main/java/com/loudent/template/ to match your new project structure.

For example, if your new package name is com.yourcompany.newproject, move the files accordingly:
```swift
src/main/java/com/yourcompany/newproject/
```

Then update the package declaration in TemplateApplication.java:
```java
package com.yourcompany.newproject;
```

Refactor Imports (Optional)
If using an IDE like IntelliJ IDEA or Eclipse, you can use the Refactor > Rename feature to rename packages automatically.

### 3. Run the Application
```sh
./gradlew bootRun
```

### 4. Run Tests
```sh
./gradlew test
```
s
### 5. Create a New Git Repository
If you are using this template for a new project:
```sh
rm -rf .git
git init
git add .
git commit -m "Initial commit from template"
git remote add origin https://github.com/your-username/your-new-repo.git
git push -u origin main
```

## Project structure
```swift
template-project/
│── src/
│   ├── main/
│   │   ├── java/com/loudent/template/
│   │   │   ├── TemplateApplication.java
│   ├── test/
│   │   ├── java/com/loudent/template/
│   │   │   ├── TemplateApplicationTest.java
│── build.gradle
│── settings.gradle
│── .gitignore
│── README.md
```

## 🔧 Customization
 - Change group in build.gradle to match your package naming convention.
 - Update application.yml with your configurations.
 - Extend this template by adding logging, database support, or security as needed.






