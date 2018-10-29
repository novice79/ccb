package freego

import org.slf4j.LoggerFactory;
import org.slf4j.Logger
import org.slf4j.Marker


inline fun <reified T> T.logger(): Logger {
    if (T::class.isCompanion) {
        return LoggerFactory.getLogger(T::class.java.enclosingClass)
    }
    return LoggerFactory.getLogger(T::class.java)
}
class ccb {
    companion object {
        val logger = logger()
    }
}
val logger = ccb.logger
