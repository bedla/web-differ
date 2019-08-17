package cz.bedla.differ.configuration

import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStores
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class DatabaseConfig(
    @Value("#{environment['databaseDir']}") val databaseDir: File
) {
    @Bean(destroyMethod = "close")
    fun persistentEntityStore(): PersistentEntityStore {
        databaseDir.mkdir()
        return PersistentEntityStores.newInstance(databaseDir)
    }
}
