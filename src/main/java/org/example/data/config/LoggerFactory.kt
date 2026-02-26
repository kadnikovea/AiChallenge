package org.example.data.config

import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Marker

object LoggerFactory {
    private var isLoggingEnabled: Boolean? = null
    
    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
    }
    
    fun getLogger(): KLogger {
        // If not explicitly set, try to load from config
        if (isLoggingEnabled == null) {
            try {
                isLoggingEnabled = ConfigLoader.load().enableLogging
            } catch (e: Exception) {
                // If config loading fails, default to true
                isLoggingEnabled = true
            }
        }
        
        return if (isLoggingEnabled == true) {
            KotlinLogging.logger {}
        } else {
            // Return a logger that does nothing
            object : KLogger {
                
                
                
                override fun getName(): String = "NOP"
                override fun trace(msg: () -> Any?) {}
                override fun trace(t: Throwable?, msg: () -> Any?) {}
                override fun trace(marker: org.slf4j.Marker?, msg: () -> Any?) {}
                override fun trace(marker: org.slf4j.Marker?, t: Throwable?, msg: () -> Any?) {}
                override fun debug(msg: () -> Any?) {}
                override fun debug(t: Throwable?, msg: () -> Any?) {}
                override fun debug(marker: org.slf4j.Marker?, msg: () -> Any?) {}
                override fun debug(marker: org.slf4j.Marker?, t: Throwable?, msg: () -> Any?) {}
                override fun info(msg: () -> Any?) {}
                override fun info(t: Throwable?, msg: () -> Any?) {}
                override fun info(marker: org.slf4j.Marker?, msg: () -> Any?) {}
                override fun info(marker: org.slf4j.Marker?, t: Throwable?, msg: () -> Any?) {}
                override fun warn(msg: () -> Any?) {}
                override fun warn(t: Throwable?, msg: () -> Any?) {}
                override fun warn(marker: org.slf4j.Marker?, msg: () -> Any?) {}
                override fun warn(marker: org.slf4j.Marker?, t: Throwable?, msg: () -> Any?) {}
                override fun error(msg: () -> Any?) {}
                override fun error(t: Throwable?, msg: () -> Any?) {}
                override fun error(marker: org.slf4j.Marker?, msg: () -> Any?) {}
                override fun error(marker: org.slf4j.Marker?, t: Throwable?, msg: () -> Any?) {}
                override fun isTraceEnabled(): Boolean = false
                override fun trace(p0: String?) {
                    
                }

                override fun trace(p0: String?, p1: Any?) {
                    
                }

                override fun trace(p0: String?, p1: Any?, p2: Any?) {
                    
                }

                override fun trace(p0: String?, vararg p1: Any?) {
                    
                }

                override fun trace(p0: String?, p1: Throwable?) {
                    
                }

                override fun isDebugEnabled(): Boolean = false
                override fun debug(p0: String?) {
                    
                }

                override fun debug(p0: String?, p1: Any?) {
                    
                }

                override fun debug(p0: String?, p1: Any?, p2: Any?) {
                    
                }

                override fun debug(p0: String?, vararg p1: Any?) {
                    
                }

                override fun debug(p0: String?, p1: Throwable?) {
                    
                }

                override fun isInfoEnabled(): Boolean = false
                override fun info(p0: String?) {
                    
                }

                override fun info(p0: String?, p1: Any?) {
                    
                }

                override fun info(p0: String?, p1: Any?, p2: Any?) {
                    
                }

                override fun info(p0: String?, vararg p1: Any?) {
                    
                }

                override fun info(p0: String?, p1: Throwable?) {
                    
                }

                override fun isWarnEnabled(): Boolean = false
                override fun warn(p0: String?) {
                    
                }

                override fun warn(p0: String?, p1: Any?) {
                    
                }

                override fun warn(p0: String?, vararg p1: Any?) {
                    
                }

                override fun warn(p0: String?, p1: Any?, p2: Any?) {
                    
                }

                override fun warn(p0: String?, p1: Throwable?) {
                    
                }

                override fun isErrorEnabled(): Boolean = false
                override fun error(p0: String?) {
                    
                }

                override fun error(p0: String?, p1: Any?) {
                    
                }

                override fun error(p0: String?, p1: Any?, p2: Any?) {
                    
                }

                override fun error(p0: String?, vararg p1: Any?) {
                    
                }

                override fun error(p0: String?, p1: Throwable?) {
                    
                }
                
                override fun isTraceEnabled(marker: org.slf4j.Marker?): Boolean = false
                override fun trace(p0: Marker?, p1: String?) {
                    
                }

                override fun trace(p0: Marker?, p1: String?, p2: Any?) {
                    
                }

                override fun trace(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
                    
                }

                override fun trace(p0: Marker?, p1: String?, vararg p2: Any?) {
                    
                }

                override fun trace(p0: Marker?, p1: String?, p2: Throwable?) {
                    
                }

                override fun isDebugEnabled(marker: org.slf4j.Marker?): Boolean = false
                override fun debug(p0: Marker?, p1: String?) {
                    
                }

                override fun debug(p0: Marker?, p1: String?, p2: Any?) {
                    
                }

                override fun debug(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
                    
                }

                override fun debug(p0: Marker?, p1: String?, vararg p2: Any?) {
                    
                }

                override fun debug(p0: Marker?, p1: String?, p2: Throwable?) {
                    
                }

                override fun isInfoEnabled(marker: org.slf4j.Marker?): Boolean = false
                override fun info(p0: Marker?, p1: String?) {
                    
                }

                override fun info(p0: Marker?, p1: String?, p2: Any?) {
                    
                }

                override fun info(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
                    
                }

                override fun info(p0: Marker?, p1: String?, vararg p2: Any?) {
                    
                }

                override fun info(p0: Marker?, p1: String?, p2: Throwable?) {
                    
                }

                override fun isWarnEnabled(marker: org.slf4j.Marker?): Boolean = false
                override fun warn(p0: Marker?, p1: String?) {
                    
                }

                override fun warn(p0: Marker?, p1: String?, p2: Any?) {
                    
                }

                override fun warn(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
                    
                }

                override fun warn(p0: Marker?, p1: String?, vararg p2: Any?) {
                    
                }

                override fun warn(p0: Marker?, p1: String?, p2: Throwable?) {
                    
                }

                override fun isErrorEnabled(marker: org.slf4j.Marker?): Boolean = false
                override fun error(p0: Marker?, p1: String?) {
                    
                }

                override fun error(p0: Marker?, p1: String?, p2: Any?) {
                    
                }

                override fun error(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
                    
                }

                override fun error(p0: Marker?, p1: String?, vararg p2: Any?) {
                    
                }

                override fun error(p0: Marker?, p1: String?, p2: Throwable?) {
                    
                }

                override fun <T : Throwable> catching(throwable: T) {}
                override fun <T : Throwable> throwing(throwable: T): T = throwable
                override fun entry(vararg argArray: Any?) {}
                override fun exit() {}
                override fun <T> exit(result: T): T = result
                override val underlyingLogger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("NOP")
            }
        }
    }
}
