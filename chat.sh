#!/bin/bash
# Запуск чата без прогресс-бара Gradle (--console=plain),
# чтобы backspace и ввод не конфликтовали с выводом
exec ./gradlew run --args="chat" --console=plain --quiet "$@"
