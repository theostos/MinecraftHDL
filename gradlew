#!/usr/bin/env sh

APP_HOME=$(cd "${0%/*}" && pwd)

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_EXEC="$JAVA_HOME/bin/java"
else
  JAVA_EXEC="java"
fi

exec "$JAVA_EXEC" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
