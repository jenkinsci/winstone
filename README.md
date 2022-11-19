# What is Winstone?
Winstone is a command line interface around Jetty 10.0.x, which implements
Servlet 4.0 (JakartaEE 8/`javax.servlet.*`), WebSocket/JSR-356, and HTTP/2 support. It is used as the default
embedded servlet container in Jenkins (via the `executable` package in the `war` module)
and can be used by any other web applications that wants to be self-contained.

## History

Winstone was [originally a from-scratch servlet container by Rick Knowles](http://winstone.sourceforge.net/)
with a good command line interface. Over time, the upstream development
has halted, and it became impractical to maintain a from-scratch servlet
implementation on our own. To reduce this maintenance burden, the gut of
Winstone has been removed and delegated to Jetty, while CLI was preserved, and
we called it Winstone 2.0.

## License
The license of Winstone inherits its original license by Rick.

The Web-App DTD files (everything in the `src/javax/servlet/resources` and
`src/javax/servlet/jsp/resources` folders) are covered by the licenses
described at the top of each file (usually as licensed by Sun Microsystems).

As of v0.8.1, all other files are dual-licensed, under either
the Lesser GNU Public License (LGPL) as described in LICENSE-LGPL.txt,
or the Common Development and Distribution License (CDDL) as described in
LICENSE-CDDL.txt.

The goal of dual-licensing is to make Winstone as attractive as possible to
distributors of commercial webapps, while ensuring everyone benefits from
any improvements. The CDDL allows free distribution with any commercial
applications, while distribution with a GPL licensed webapp is also possible
under the LGPL. If you are unclear about which license applies to an
application you wish to distribute or sell, please contact me.

## Using Winstone
To run a single war file:

    java -jar winstone.jar --warfile=<location of warfile> (+ other options)

To run a directory full of war files:

    java -jar winstone.jar --webappsDir=<location of webapps directory> (+ other options)

To run locally exploded web archive:

    java -jar winstone.jar --webroot=<location of webroot> (+ other options)

To run different web applications for diffent virtual hosts:

    java -jar winstone.jar --hostsDir=<location of hosts directory> (+ other options)


## Command-line options

    Winstone Servlet Engine, (c) 2003-2006 Rick Knowles
    Usage: java winstone.jar [--option=value] [--option=value] [etc]
    
    Required options: either --webroot OR --warfile OR --webappsDir OR --hostsDir
       --webroot                = set document root folder.
       --warfile                = set location of warfile to extract from.
       --webappsDir             = set directory for multiple webapps to be deployed from
    Other options:
       --javaHome               = Override the JAVA_HOME variable
       --config                 = load configuration properties from here. Default is ./winstone.properties
       --prefix                 = add this prefix to all URLs (eg http://localhost:8080/prefix/resource). Default is none
       --commonLibFolder        = folder for additional jar files. Default is ./lib
    
       --extraLibFolder         = folder for additional jar files to add to Jetty classloader
    
       --logfile                = redirect log messages to this file
       --logThrowingLineNo      = show the line no that logged the message (slow). Default is false
       --logThrowingThread      = show the thread that logged the message. Default is false
       --debug                  = set the level of debug msgs (1-9). Default is 5 (INFO level)
    
       --httpPort               = set the http listening port. -1 to disable, Default is 8080
       --httpListenAddress      = set the http listening address. Default is all interfaces
       --httpKeepAliveTimeout   = how long idle HTTP keep-alive connections are kept around (in ms; default 30000)?
       --httpsPort              = set the https listening port. -1 to disable, Default is disabled
       --httpsListenAddress     = set the https listening address. Default is all interfaces
       --httpsKeepAliveTimeout  = how long idle HTTPS keep-alive connections are kept around (in ms; default 30000)?
       --httpsKeyStore          = the location of the SSL KeyStore file. Default is ./winstone.ks
       --httpsKeyStorePassword  = the password for the SSL KeyStore file. Default is null
       --httpsKeyManagerType    = the SSL KeyManagerFactory type (eg SunX509, IbmX509). Default is SunX509
       --httpsRedirectHttp      = redirect http requests to https (requires both --httpPort and --httpsPort)
       --http2Port              = set the http2 listening port. -1 to disable, Default is disabled
       --httpsSniHostCheck      = if the SNI Host name must match when there is an SNI certificate. Check disabled per default
       --httpsSniRequired       = if a SNI certificate is required. Disabled per default
       --http2ListenAddress     = set the http2 listening address. Default is all interfaces
       --excludeCipherSuites    = set the ciphers to exclude (comma separated, use blank quote " " to exclude none) (default is 
                               // Exclude weak / insecure ciphers 
                               "^.*_(MD5|SHA|SHA1)$", 
                               // Exclude ciphers that don't support forward secrecy 
                               "^TLS_RSA_.*$", 
                               // The following exclusions are present to cleanup known bad cipher 
                               // suites that may be accidentally included via include patterns. 
                               // The default enabled cipher list in Java will not include these 
                               // (but they are available in the supported list). 
                               "^SSL_.*$", 
                               "^.*_NULL_.*$", 
                               "^.*_anon_.*$" 
       --controlPort            = set the shutdown/control port. -1 to disable, Default disabled
    
       --sessionTimeout         = set the http session timeout value in minutes. Default to what webapp specifies, and then to 60 minutes
       --sessionEviction        = set the session eviction timeout for idle sessions in seconds. Default value is 180. -1 never evict, 0 evict on exit
       --mimeTypes=ARG          = define additional MIME type mappings. ARG would be EXT=MIMETYPE:EXT=MIMETYPE:...
                                  (e.g., xls=application/vnd.ms-excel:wmf=application/x-msmetafile)
       --requestHeaderSize=N    = set the maximum size in bytes of the request header. Default is 8192.
       --responseHeaderSize=N   = set the maximum size in bytes of the response header. Default is 8192.
       --maxParamCount=N        = set the max number of parameters allowed in a form submission to protect
                                  against hash DoS attack (oCERT #2011-003). Default is 10000.
       --useJmx                 = Enable Jetty Jmx
       --qtpMaxThreadsCount     = max threads number when using Jetty Queued Thread Pool
       --jettyAcceptorsCount    = Jetty Acceptors number
       --jettySelectorsCount    = Jetty Selectors number
       --usage / --help         = show this message
     Security options:
       --realmClassName               = Set the realm class to use for user authentication. Defaults to ArgumentsRealm class
    
       --argumentsRealm.passwd.<user> = Password for user <user>. Only valid for the ArgumentsRealm realm class
       --argumentsRealm.roles.<user>  = Roles for user <user> (comma separated). Only valid for the ArgumentsRealm realm class
    
       --fileRealm.configFile         = File containing users/passwds/roles. Only valid for the FileRealm realm class
    
     Access logging:
       --accessLoggerClassName        = Set the access logger class to use for user authentication. Defaults to disabled
       --simpleAccessLogger.format    = The log format to use. Supports combined/common/resin/custom (SimpleAccessLogger only)
       --simpleAccessLogger.file      = The location pattern for the log file(SimpleAccessLogger only)

## Configuration file
You don't really need a config file, but sometimes it's handy to
be able to use the same settings each time without running through
the command history.

Winstone looks for a config file `winstone.properties` in the current directory
(or in the location specified with `--config`) at startup. It loads
the properties in this file, overrides them with any supplied command
line properties, and then starts itself.

This is just intended as a handy feature for people who want to cache
regular startup options, rather than using batch files.

## Deployment choices
The *simplest way* to use winstone is with a single webapp. To do this,
just supply the warfile or webroot directory as an argument:

* `java -jar winstone.jar <webroot or warfile>`, (this method auto-detects the type) or
* `java -jar winstone.jar --webroot=<webroot>`, or
* `java -jar winstone.jar --warfile=<warfile>`

If you need to support *multiple webapps*, use the `--webappsDir` switch,
to which you pass a directory that contains multiple warfiles/webroots.

* `java -jar winstone.jar --webappsDir=<dir containing multiple webroots>`

The directory becomes the prefix name for that webapp (so hello becomes `/hello`, etc).
The directory named ROOT becomes the no-prefix webapp.

So, for example, if you had a directory `/usr/local/webapps` which contained
sub-directories `ROOT` and `test`, if you executed
`java -jar winstone.jar --webappsDir=/usr/local/webapps`, you would find that
the test folder would act as a webroot for requests prefixed with
`/test`, while other requests would go to the webapp in the `ROOT` folder

If you need multiple hosts (sometimes called name-based virtual hosting),
there is an option `--hostsDir`. This acts in a similar way to the `--webappsDir` switch,
but it defines an extra level of sub-directories, the top being a per-host directory
and the second a per-webapp directory as with `--webappsDir`.

The directory name becomes the host name: that is, a directory named `www.blah.com`
will only serve requests with the host header www.blah.com, unless it is the default
host. If a directory named "default" is found, it becomes the default host.
If no default directory is found, the first directory in the list (alphabetically)
becomes the default host.

* `java -jar winstone.jar --hostsDir=<dir containing multiple host directories>`

## Development
If you have some unit test failures you may add an interface/ip alias such

``` sudo ifconfig lo0 alias 127.0.0.2 ```

## Changelog

See [GitHub releases](https://github.com/jenkinsci/winstone/releases),
or for older versions [this page](CHANGELOG.md).
