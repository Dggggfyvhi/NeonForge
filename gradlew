#!/bin/sh
# Minimal POSIX wrapper for Gradle Wrapper
set -e
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
exec "$JAVACMD" -Xmx64m -Xms64m -classpath "./gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
