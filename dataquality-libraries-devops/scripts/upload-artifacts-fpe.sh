#!/bin/sh
TALEND_UPDATE_LINK="https://talend-update.talend.com/nexus/content/repositories/libraries/"

ARTIFACT_MVN_URI_LIST="com.idealista/format-preserving-encryption/1.0.0"


for element in ${ARTIFACT_MVN_URI_LIST}    
do
    echo "------------------------------------------------------"
    echo "|     " ${element} "    |"
    echo "------------------------------------------------------"

    group_id=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\1/p'`
    artifact_id=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\2/p'`
    artifact_version=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\3/p'`

    # download from artifacts-zl
    mvn dependency:get \
        -DrepoUrl=${NEXUS_RELEASE_LINK} \
        -DgroupId=${group_id} \
        -DartifactId=${artifact_id} \
        -Dversion=${artifact_version} \
        -Dpackaging=jar \
        -Ddest=./extras/${artifact_id}/${artifact_id}-${artifact_version}.jar

    sed -i '' -e 's/<groupId>.*<\/groupId>/<groupId>'${group_id}'<\/groupId>/g' \
      ./extras/${artifact_id}/pom.xml
    sed -i '' -e 's/<artifactId>.*<\/artifactId>/<artifactId>'${artifact_id}'<\/artifactId>/g' \
      ./extras/${artifact_id}/pom.xml
    sed -i '' -e 's/<version>.*<\/version>/<version>'${artifact_version}'<\/version>/g' \
      ./extras/${artifact_id}/pom.xml

    # upload to talend-update
    mvn deploy:deploy-file \
        -Durl=${TALEND_UPDATE_LINK} \
        -DrepositoryId=talend-update \
        -DgroupId=${group_id} \
        -DartifactId=${artifact_id} \
        -Dversion=${artifact_version} \
        -DpomFile=./extras/${artifact_id}/pom.xml \
        -Dfile=./extras/${artifact_id}/${artifact_id}-${artifact_version}.jar
done
