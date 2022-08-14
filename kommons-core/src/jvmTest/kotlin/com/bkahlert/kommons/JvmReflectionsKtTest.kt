package com.bkahlert.kommons

import com.bkahlert.kommons.SealedInterface.SealedClass
import com.bkahlert.kommons.SealedInterface.SealedClass.SealedObjectInstance2
import com.bkahlert.kommons.SealedInterface.SealedClass.SealedObjectInstance3
import com.bkahlert.kommons.SealedInterface.SealedObjectInstance1
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class JvmReflectionsKtTest {

    @Test fun all_sealed_subclasses() = testAll {
        JvmReflectionsKtTest::class.allSealedSubclasses.shouldBeEmpty()
        SealedInterface::class.allSealedSubclasses.shouldContainExactly(
            SealedClass::class,
            SealedObjectInstance2::class,
            SealedObjectInstance3::class,
            SealedObjectInstance1::class,
        )
    }

    @Test fun all_sealed_object_instances() = testAll {
        JvmReflectionsKtTest::class.allSealedObjectInstances.shouldBeEmpty()
        SealedInterface::class.allSealedObjectInstances.shouldContainExactly(
            SealedObjectInstance2,
            SealedObjectInstance3,
            SealedObjectInstance1,
        )
    }
}

private sealed interface SealedInterface {
    sealed class SealedClass : SealedInterface {
        object SealedObjectInstance2 : SealedClass()
        object SealedObjectInstance3 : SealedClass()
    }

    object SealedObjectInstance1 : SealedInterface
}
