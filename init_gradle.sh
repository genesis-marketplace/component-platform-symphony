#!/usr/bin/env sh

if [ ! -d "$HOME/.gradle" ]; then
  mkdir "$HOME/.gradle"
fi

if [ -f "$HOME/.gradle/gradle.properties" ]; then
  rm "$HOME/.gradle/gradle.properties"
fi

touch $HOME/.gradle/gradle.properties

echo genesisArtifactoryUser="$genesisArtifactoryUser" >> $HOME/.gradle/gradle.properties
echo genesisArtifactoryPassword="$genesisArtifactoryPassword" >> $HOME/.gradle/gradle.properties

echo systemProp.org.gradle.internal.http.connectionTimeout=180000 >> $HOME/.gradle/gradle.properties
echo systemProp.org.gradle.internal.http.socketTimeout=180000 >> $HOME/.gradle/gradle.properties
echo org.gradle.jvmargs=-Xmx4096m "-XX:MaxMetaspaceSize=2048m" >> $HOME/.gradle/gradle.properties
