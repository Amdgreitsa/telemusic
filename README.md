# Telemusic / Telegram для Android

`Telemusic` — это форк Android-клиента Telegram, основанный на исходниках официального приложения.  
Проект ориентирован на мобильный клиент с поддержкой стандартного Telegram-функционала и расширений, включая отдельные сборки/варианты приложения.

---

## О проекте

- Платформа: **Android**
- Язык и стек: **Java + Android Gradle Plugin + NDK/CMake**
- Базовый модуль логики: `TMessagesProj`
- Основной app-модуль: `TMessagesProj_App`
- Дополнительные app-модули: Huawei / Standalone / HockeyApp / Tests

Проект использует Telegram API и протокол MTProto. Если вы делаете собственную сборку/публикацию, используйте **свои** ключи, идентификаторы и конфигурацию сервисов.

---

## Структура репозитория

- `TMessagesProj/` — основной модуль (ядро клиента, UI, ресурсы, JNI/NDK часть).
- `TMessagesProj_App/` — основной Android-приложение модуль (обычная сборка).
- `TMessagesProj_AppHuawei/` — вариант сборки для Huawei-сервисов.
- `TMessagesProj_AppStandalone/` — standalone-ветка сборки.
- `TMessagesProj_AppHockeyApp/` — вариант со специфичной интеграцией дистрибуции.
- `TMessagesProj_AppTests/` — тестовый app-модуль.
- `Tools/` — служебные инструменты/скрипты.
- `gradlew`, `gradle/` — Gradle wrapper и конфигурация сборки.

---

## Требования к окружению

Рекомендуемые версии для текущего состояния проекта:

- **Android Studio**: актуальная стабильная версия
- **Gradle Wrapper**: `8.7` (используется автоматически через `./gradlew`)
- **AGP (Android Gradle Plugin)**: `8.6.1`
- **JDK для Gradle**: **21**
- **Android SDK Platform**: 35
- **Android Build Tools**: 35.0.0
- **NDK**: 21.4.7075529
- **CMake**: 3.10.2

> Важно: использовать именно `gradlew` из репозитория, а не системный gradle.

---

## Быстрый старт

1. Клонируйте репозиторий:

```bash
git clone <URL_ВАШЕГО_РЕПОЗИТОРИЯ>
cd Telemusic
```

2. Настройте Android SDK (файл `local.properties` в корне):

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Для Windows пример:

```properties
sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
```

3. Проверьте `gradle.properties` и параметры подписи (если планируется release-сборка):

- `RELEASE_KEY_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_STORE_PASSWORD`

4. При необходимости замените заглушки своими файлами/секретами:

- `release.keystore`
- `google-services.json`
- значения в `BuildVars.java`/других конфигурациях

5. Откройте проект в Android Studio через **Open** (не Import).

---

## Сборка проекта

### Через Android Studio

- Откройте окно **Build Variants**.
- Для основного приложения выберите модуль `TMessagesProj_App` и вариант, например `afatDebug`.
- Выполните **Build > Make Project** или **Assemble**.

### Через CLI

Основные команды:

```bash
./gradlew :TMessagesProj_App:assembleAfatDebug
./gradlew :TMessagesProj_App:assembleRelease
```

Для Huawei-варианта:

```bash
./gradlew :TMessagesProj_AppHuawei:assembleAfatDebug
```

Если нужно проверить только основной модуль-ядро:

```bash
./gradlew :TMessagesProj:assembleDebug
```

---

## Типичные проблемы и решения

### 1) `SDK location not found`

Причина: не настроен `local.properties` или переменные `ANDROID_HOME`/`ANDROID_SDK_ROOT`.

Решение: укажите корректный `sdk.dir` в `local.properties`.

### 2) Ошибки Gradle JDK / несовместимой Java

Причина: используется неподходящая версия JDK.

Решение:
- в Android Studio выставьте **Gradle JDK = JDK 21** (или Embedded JDK, совместимый с проектом);
- убедитесь, что сборка запускается через `gradlew`.

### 3) Ошибки `R.string ... cannot find symbol`

Причина: конфликты/дубли или повреждённые XML в ресурсах `res/values`.

Решение:
- проверьте XML на валидность;
- убедитесь, что в `res/values/*.xml` нет дублей одинаковых `<string name="...">`;
- после правок сделайте `Clean Project` и пересоберите.

### 4) Ошибка merge ресурсов: `Found item String/... more than one time`

Причина: один и тот же ключ объявлен дважды в default `values`.

Решение: оставьте ключ только в одном файле `res/values/*.xml`.

---

## API и лицензии

- Документация Telegram API: https://core.telegram.org/api
- Документация MTProto: https://core.telegram.org/mtproto
- Рекомендации по безопасности: https://core.telegram.org/mtproto/security_guidelines

Если вы делаете собственное приложение на основе этих исходников:

1. Получите собственный `api_id` / `api_hash`.
2. Не вводите пользователей в заблуждение названием/брендингом.
3. Используйте собственные ключи и инфраструктуру.
4. Соблюдайте условия лицензии и публикуйте изменения там, где это требуется.

---

## Локализация

Локализация Telegram-экосистемы ведётся через платформу переводов:

https://translations.telegram.org/

Если вы поддерживаете собственные строки в форке — следите за консистентностью ключей и отсутствием дублей в `res/values`.

---

## Рекомендации для разработчиков форка

- Перед крупными изменениями прогоняйте хотя бы:

```bash
./gradlew :TMessagesProj_App:assembleAfatDebug
```

- Для изменений ресурсов дополнительно проверяйте:
  - валидность XML,
  - отсутствие дублей ключей в `values/*.xml`.

- Для reproducible/release-сборок обязательно заменяйте тестовые/заглушечные файлы на свои.

---

Если нужна отдельная инструкция под **Windows** (с точными путями, настройкой JDK в Android Studio и сборкой APK/AAB), можно добавить её отдельным разделом в этот README.
