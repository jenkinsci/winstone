
########################################################################################
# Makefile for GCJ compilation of Winstone Servlet Container to native binary
#
# Currently works under linux only - mingw compilation unsupported
#
# Author: Rick Knowles
#
########################################################################################

CC = gcj
CFLAGS = -ffast-math -funroll-loops

WINSTONE_JAVA_BASE = ./winstone/src/java
WINSTONE_RES_BASE = ./winstone/src/conf
CRIMSON_BASE = ./crimson-src

WINSTONE_LITE_O = winstone_lite.o winstone_res1.o
WINSTONE_AJP_O = winstone_ajp.o winstone_ajp_res1.o
WINSTONE_AUTH_O = winstone_auth.o winstone_realm.o winstone_auth_res1.o winstone_realm_res1.o
WINSTONE_CLUSTER_O = winstone_cluster.o winstone_cluster_res1.o
WINSTONE_CLASSLOADER_O = winstone_classloader.o winstone_classloader_res1.o
WINSTONE_JNDI_O = winstone_jndi.o winstone_jndi_res1.o winstone_jndi_res2.o
WINSTONE_SSL_O = winstone_ssl.o winstone_ssl_res1.o
WINSTONE_INVOKER_O = winstone_invoker.o winstone_invoker_res1.o
SERVLET_O = servlet.o servlet_res1.o servlet_res2.o
CRIMSON_O = crimson.o crimson_res1.o crimson_res2.o javax_xml.o xml_api.o
JSP_API_O = jsp_api.o jsp_api_res1.o jsp_api_res2.o jsp_api_res3.o jsp_api_res4.o jsp_api_res5.o jsp_api_res6.o

all:	$(WINSTONE_LITE_O) $(WINSTONE_AJP_O) $(WINSTONE_AUTH_O) $(WINSTONE_CLUSTER_O) $(WINSTONE_JNDI_O) $(WINSTONE_CLASSLOADER_O) $(WINSTONE_SSL_O) $(WINSTONE_INVOKER_O) $(SERVLET_O) $(CRIMSON_O) $(JSP_API_O)
	$(CC) $(CFLAGS) --main=winstone.Launcher -o winstone_lite $(WINSTONE_LITE_O) $(SERVLET_O) $(CRIMSON_O)
	$(CC) $(CFLAGS) --main=winstone.Launcher -o winstone_full $(WINSTONE_LITE_O) $(SERVLET_O) $(CRIMSON_O) $(WINSTONE_AUTH_O) $(WINSTONE_AJP_O) $(WINSTONE_CLUSTER_O) $(WINSTONE_JNDI_O) $(WINSTONE_CLASSLOADER_O) $(WINSTONE_SSL_O) $(WINSTONE_INVOKER_O) $(JSP_API_O)

winstone_lite.o: $(WINSTONE_JAVA_BASE)/winstone/*.java
	$(CC) $(CFLAGS) -o winstone_lite.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/*.java
winstone_res1.o: $(WINSTONE_JAVA_BASE)/winstone/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_res1.o --resource=winstone/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/LocalStrings.properties

winstone_ajp.o: $(WINSTONE_JAVA_BASE)/winstone/ajp13/*.java
	$(CC) $(CFLAGS) -o winstone_ajp.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/ajp13/*.java
winstone_ajp_res1.o: $(WINSTONE_JAVA_BASE)/winstone/ajp13/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_ajp_res1.o --resource=winstone/ajp13/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/ajp13/LocalStrings.properties

winstone_auth.o: $(WINSTONE_JAVA_BASE)/winstone/auth/*.java
	$(CC) $(CFLAGS) -o winstone_auth.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/auth/*.java
winstone_auth_res1.o: $(WINSTONE_JAVA_BASE)/winstone/auth/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_auth_res1.o --resource=winstone/auth/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/auth/LocalStrings.properties

winstone_realm.o: $(WINSTONE_JAVA_BASE)/winstone/realm/*.java
	$(CC) $(CFLAGS) -o winstone_realm.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/realm/*.java
winstone_realm_res1.o: $(WINSTONE_JAVA_BASE)/winstone/realm/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_realm_res1.o --resource=winstone/realm/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/realm/LocalStrings.properties

winstone_cluster.o: $(WINSTONE_JAVA_BASE)/winstone/cluster/*.java
	$(CC) $(CFLAGS) -o winstone_cluster.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/cluster/*.java
winstone_cluster_res1.o: $(WINSTONE_JAVA_BASE)/winstone/cluster/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_cluster_res1.o --resource=winstone/cluster/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/cluster/LocalStrings.properties

winstone_classloader.o: $(WINSTONE_JAVA_BASE)/winstone/classLoader/*.java
	$(CC) $(CFLAGS) -o winstone_classloader.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/classLoader/*.java
winstone_classloader_res1.o: $(WINSTONE_JAVA_BASE)/winstone/classLoader/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_classloader_res1.o --resource=winstone/classLoader/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/classLoader/LocalStrings.properties

winstone_ssl.o: $(WINSTONE_JAVA_BASE)/winstone/ssl/*.java
	$(CC) $(CFLAGS) -o winstone_ssl.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/ssl/*.java
winstone_ssl_res1.o: $(WINSTONE_JAVA_BASE)/winstone/ssl/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_ssl_res1.o --resource=winstone/ssl/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/ssl/LocalStrings.properties

winstone_invoker.o: $(WINSTONE_JAVA_BASE)/winstone/invoker/*.java
	$(CC) $(CFLAGS) -o winstone_invoker.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c $(WINSTONE_JAVA_BASE)/winstone/invoker/*.java
winstone_invoker_res1.o: $(WINSTONE_JAVA_BASE)/winstone/invoker/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_invoker_res1.o --resource=winstone/invoker/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/invoker/LocalStrings.properties

winstone_jndi.o: $(WINSTONE_JAVA_BASE)/winstone/jndi/*.java \
                 $(WINSTONE_JAVA_BASE)/winstone/jndi/java/*.java \
                 $(WINSTONE_JAVA_BASE)/winstone/jndi/resourceFactories/*.java
	$(CC) $(CFLAGS) -o winstone_jndi.o --classpath=$(WINSTONE_JAVA_BASE):$(CRIMSON_BASE) -c \
                 $(WINSTONE_JAVA_BASE)/winstone/jndi/*.java \
                 $(WINSTONE_JAVA_BASE)/winstone/jndi/java/*.java \
                 $(WINSTONE_JAVA_BASE)/winstone/jndi/resourceFactories/*.java
winstone_jndi_res1.o: $(WINSTONE_JAVA_BASE)/winstone/jndi/LocalStrings.properties
	$(CC) $(CFLAGS) -o winstone_jndi_res1.o --resource=winstone/jndi/LocalStrings.properties -c $(WINSTONE_JAVA_BASE)/winstone/jndi/LocalStrings.properties
winstone_jndi_res2.o: $(WINSTONE_RES_BASE)/jndi.properties
	$(CC) $(CFLAGS) -o winstone_jndi_res2.o --resource=jndi.properties -c $(WINSTONE_RES_BASE)/jndi.properties

servlet.o: $(WINSTONE_JAVA_BASE)/javax/servlet/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/http/*.java
	$(CC) $(CFLAGS) -o servlet.o --classpath=$(WINSTONE_JAVA_BASE) -c $(WINSTONE_JAVA_BASE)/javax/servlet/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/http/*.java
servlet_res1.o: $(WINSTONE_RES_BASE)/javax/servlet/resources/web-app_2_2.dtd
	$(CC) $(CFLAGS) -o servlet_res1.o --resource=javax/servlet/resources/web-app_2_2.dtd -c $(WINSTONE_RES_BASE)/javax/servlet/resources/web-app_2_2.dtd
servlet_res2.o: $(WINSTONE_RES_BASE)/javax/servlet/resources/web-app_2_3.dtd
	$(CC) $(CFLAGS) -o servlet_res2.o --resource=javax/servlet/resources/web-app_2_3.dtd -c $(WINSTONE_RES_BASE)/javax/servlet/resources/web-app_2_3.dtd

# Add JSP API classes here
jsp_api.o: $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/tagext/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/el/*.java
	$(CC) $(CFLAGS) -o jsp_api.o --classpath=$(WINSTONE_JAVA_BASE) -c $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/tagext/*.java $(WINSTONE_JAVA_BASE)/javax/servlet/jsp/el/*.java
jsp_api_res1.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jsp_2_0.xsd
	$(CC) $(CFLAGS) -o jsp_api_res1.o --resource=javax/servlet/jsp/resources/jsp_2_0.xsd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jsp_2_0.xsd
jsp_api_res2.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jspxml.dtd
	$(CC) $(CFLAGS) -o jsp_api_res2.o --resource=javax/servlet/jsp/resources/jspxml.dtd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jspxml.dtd
jsp_api_res3.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jspxml.xsd
	$(CC) $(CFLAGS) -o jsp_api_res3.o --resource=javax/servlet/jsp/resources/jspxml.xsd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/jspxml.xsd
jsp_api_res4.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd
	$(CC) $(CFLAGS) -o jsp_api_res4.o --resource=javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd
jsp_api_res5.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd
	$(CC) $(CFLAGS) -o jsp_api_res5.o --resource=javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd
jsp_api_res6.o: $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd
	$(CC) $(CFLAGS) -o jsp_api_res6.o --resource=javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd -c $(WINSTONE_RES_BASE)/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd

javax_xml.o: $(CRIMSON_BASE)/javax/xml/*/*.java
	$(CC) $(CFLAGS) -o javax_xml.o -c $(CRIMSON_BASE)/javax/xml/*/*.java

xml_api.o: $(CRIMSON_BASE)/org/w3c/dom/*.java $(CRIMSON_BASE)/org/xml/sax/*.java $(CRIMSON_BASE)/org/xml/sax/ext/*.java $(CRIMSON_BASE)/org/xml/sax/helpers/*.java
	$(CC) $(CFLAGS) -o xml_api.o -c $(CRIMSON_BASE)/org/w3c/dom/*.java $(CRIMSON_BASE)/org/xml/sax/*.java $(CRIMSON_BASE)/org/xml/sax/ext/*.java $(CRIMSON_BASE)/org/xml/sax/helpers/*.java

crimson.o: $(CRIMSON_BASE)/org/apache/crimson/*/*.java
	$(CC) $(CFLAGS) -o crimson.o --classpath=$(CRIMSON_BASE) -c $(CRIMSON_BASE)/org/apache/crimson/*/*.java
crimson_res1.o: $(CRIMSON_BASE)/org/apache/crimson/parser/resources/Messages.properties
	$(CC) $(CFLAGS) -o crimson_res1.o --resource=org/apache/crimson/parser/resources/Messages.properties -c $(CRIMSON_BASE)/org/apache/crimson/parser/resources/Messages.properties
crimson_res2.o: $(CRIMSON_BASE)/org/apache/crimson/tree/resources/Messages.properties
	$(CC) $(CFLAGS) -o crimson_res2.o --resource=org/apache/crimson/tree/resources/Messages.properties -c $(CRIMSON_BASE)/org/apache/crimson/tree/resources/Messages.properties

clean:
	rm -f *.o
