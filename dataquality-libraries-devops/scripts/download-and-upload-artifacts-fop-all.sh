#!/bin/sh
TALEND_UPDATE_LINK="https://talend-update.talend.com/nexus/content/repositories/libraries/"

ARTIFACT_MVN_URI_LIST="org.apache.xmlgraphics/fop/2.3 \
 org.apache.xmlgraphics/batik-all/1.10 \
 org.apache.xmlgraphics/xmlgraphics-commons/2.3 \
 org.apache.avalon.framework/avalon-framework-api/4.3.1"


for element in ${ARTIFACT_MVN_URI_LIST}    
do
    echo "------------------------------------------------------"
    echo "|     " ${element} "    |"
    echo "------------------------------------------------------"

    group_id=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\1/p'`
    artifact_id=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\2/p'`
    artifact_version=`echo "${element}" | sed -n 's/\([^\/]*\)\/\([^\/]*\)\/\([^\/]*\)/\3/p'`
    mkdir -p "./extras/${artifact_id}/"
    cat ./extras/pom_template.xml > "./extras/${artifact_id}/pom.xml"

    # download from artifacts-zl
    mvn dependency:get \
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
