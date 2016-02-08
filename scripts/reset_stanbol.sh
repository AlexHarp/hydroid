cd /var/tmp
sudo service tomcat7 stop
sudo rm -rf /usr/share/tomcat7/stanbol
sudo service tomcat7 start
until $(curl --output /dev/null --silent --head --fail http://localhost:8080/stanbol); do
	printf '.'
	sleep 2s
done
sleep 2s
# Copy GA.solrindex.zip to stanbol datafiles location
sudo cp /var/tmp/GA.solrindex.zip /usr/share/tomcat7/stanbol/datafiles/GA.solrindex.zip
# post bundle to OSGi
curl -v -F 'action=install' -F 'bundlestart=start' -F 'refreshPackages=refresh' -F 'bundlestartlevel=20' -F 'bundlefile=@/var/tmp/org.apache.stanbol.data.site.GA-1.0.0.jar' http://localhost:8080/stanbol/system/console/bundles -H 'Authorization: Basic YWRtaW46YWRtaW4=' -H 'Cookie: felix-webconsole-locale=en;' -H 'Connection: keep-alive' -H 'Pragma: no-cache' -H 'Upgrade-Insecure-Requests: 1' -H 'Content-Type: multipart/form-data;' -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8' -H 'Cache-Control: no-cache'
# Post new EntityLinking Engine to OSGi
curl --globoff http://localhost:8080/stanbol/system/console/configMgr/\[Temporary%20PID%20replaced%20by%20real%20PID%20upon%20save\] -H 'Cookie: felix-webconsole-locale=en; __test=1' -H 'Origin: http://localhost:8080/stanbol' -H 'Accept-Encoding: gzip, deflate' -H 'Accept-Language: en,en-AU;q=0.8' -H 'Authorization: Basic YWRtaW46YWRtaW4=' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' -H 'Pragma: no-cache' -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' -H 'Accept: */*' -H 'Cache-Control: no-cache' -H 'DNT: 1' --data 'apply=true&factoryPid=org.apache.stanbol.enhancer.engines.entityhublinking.EntityhubLinkingEngine&action=ajaxConfigManager&%24location=&stanbol.enhancer.engine.name=GALinking&enhancer.engines.linking.entityhub.siteId=GA&enhancer.engines.linking.labelField=rdfs%3Alabel&enhancer.engines.linking.caseSensitive=false&enhancer.engines.linking.typeField=rdf%3Atype&enhancer.engines.linking.entityTypes=&enhancer.engines.linking.redirectField=rdfs%3AseeAlso&enhancer.engines.linking.redirectMode=ADD_VALUES&enhancer.engines.linking.minSearchTokenLength=3&enhancer.engines.linking.minTokenScore=0.7&enhancer.engines.linking.suggestions=3&enhancer.engines.linking.includeSimilarScore=false&enhancer.engines.linking.properNounsState=false&enhancer.engines.linking.processedLanguages=*%3Blmmtip%3Buc%3DLINK%3Bprob%3D0.75%3Bpprob%3D0.75&enhancer.engines.linking.processedLanguages=de%3Buc%3DMATCH&enhancer.engines.linking.processedLanguages=es%3Blc%3DNoun&enhancer.engines.linking.processedLanguages=nl%3Blc%3DNoun&enhancer.engines.linking.defaultMatchingLanguage=&enhancer.engines.linking.typeMappings=dbp-ont%3AOrganisation%3B+dbp-ont%3ANewspaper%3B+schema%3AOrganization+%3E+dbp-ont%3AOrganisation&enhancer.engines.linking.typeMappings=dbp-ont%3APerson%3B+foaf%3APerson%3B+schema%3APerson+%3E+dbp-ont%3APerson&enhancer.engines.linking.typeMappings=dbp-ont%3APlace%3B+schema%3APlace+%3E+dbp-ont%3APlace&enhancer.engines.linking.typeMappings=dbp-ont%3AWork%3B+schema%3ACreativeWork+%3E+dbp-ont%3AWork&enhancer.engines.linking.typeMappings=dbp-ont%3AEvent%3B+schema%3AEvent+%3E+dbp-ont%3AEvent&enhancer.engines.linking.typeMappings=schema%3AProduct+%3E+schema%3AProduct&enhancer.engines.linking.typeMappings=skos%3AConcept+%3E+skos%3AConcept&enhancer.engines.linking.dereference=false&enhancer.engines.linking.dereferenceFields=rdfs%3Acomment&enhancer.engines.linking.dereferenceFields=geo%3Alat&enhancer.engines.linking.dereferenceFields=geo%3Along&enhancer.engines.linking.dereferenceFields=foaf%3Adepiction&enhancer.engines.linking.dereferenceFields=dbp-ont%3Athumbnail&propertylist=stanbol.enhancer.engine.name%2Cenhancer.engines.linking.entityhub.siteId%2Cenhancer.engines.linking.labelField%2Cenhancer.engines.linking.caseSensitive%2Cenhancer.engines.linking.typeField%2Cenhancer.engines.linking.entityTypes%2Cenhancer.engines.linking.redirectField%2Cenhancer.engines.linking.redirectMode%2Cenhancer.engines.linking.minSearchTokenLength%2Cenhancer.engines.linking.minTokenScore%2Cenhancer.engines.linking.suggestions%2Cenhancer.engines.linking.includeSimilarScore%2Cenhancer.engines.linking.properNounsState%2Cenhancer.engines.linking.processedLanguages%2Cenhancer.engines.linking.defaultMatchingLanguage%2Cenhancer.engines.linking.typeMappings%2Cenhancer.engines.linking.dereference%2Cenhancer.engines.linking.dereferenceFields' --compressed
# Create new chain named `hydroid`
curl --globoff http://localhost:8080/stanbol/system/console/configMgr/\[Temporary%20PID%20replaced%20by%20real%20PID%20upon%20save\] -H 'Cookie: felix-webconsole-locale=en;' -H 'Accept-Encoding: gzip, deflate' -H 'Accept-Language: en,en-AU;q=0.8' -H 'Authorization: Basic YWRtaW46YWRtaW4=' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' -H 'Pragma: no-cache' -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' -H 'Accept: */*' -H 'Cache-Control: no-cache' -H 'DNT: 1' --data 'apply=true&factoryPid=org.apache.stanbol.enhancer.chain.weighted.impl.WeightedChain&action=ajaxConfigManager&%24location=&stanbol.enhancer.chain.name=hydroid&stanbol.enhancer.chain.weighted.chain=tika%3Boptional&stanbol.enhancer.chain.weighted.chain=langdetect&stanbol.enhancer.chain.weighted.chain=opennlp-sentence&stanbol.enhancer.chain.weighted.chain=opennlp-token&stanbol.enhancer.chain.weighted.chain=opennlp-pos&stanbol.enhancer.chain.weighted.chain=opennlp-ner&stanbol.enhancer.chain.weighted.chain=dbpediaLinking&stanbol.enhancer.chain.weighted.chain=entityhubExtraction&stanbol.enhancer.chain.weighted.chain=GALinking&stanbol.enhancer.chain.weighted.chain=dbpedia-dereference&stanbol.enhancer.chain.chainproperties=&propertylist=stanbol.enhancer.chain.name%2Cstanbol.enhancer.chain.weighted.chain%2Cstanbol.enhancer.chain.chainproperties' --compressed
newPassword=$(uuidgen)
curl http://localhost:8080/stanbol/user-management/store-user -H 'Pragma: no-cache' -H 'Accept-Encoding: gzip, deflate' -H 'Accept-Language: en,en-AU;q=0.8' -H 'Authorization: Basic YWRtaW46YWRtaW4=' -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' -H 'Accept: text/html, */*; q=0.01' -H 'Cache-Control: no-cache' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' -H 'DNT: 1' --data 'currentLogin=admin&newLogin=admin&fullName=&email=noreply%40clerezza.org&password="${newPassword}"&roles%5B%5D=BasePermissionsRole&permissions%5B%5D=(java.security.AllPermission+%22%22+%22%22)&permissions%5B%5D=' --compressed