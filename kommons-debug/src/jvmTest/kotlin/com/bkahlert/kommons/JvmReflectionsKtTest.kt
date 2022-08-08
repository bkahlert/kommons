package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class JvmReflectionsKtTest {

    @Test fun all_sealed_subclasses() = testAll {
        JvmReflectionsKtTest::class.allSealedSubclasses.shouldBeEmpty()
        SealedInterface::class.allSealedSubclasses.shouldContainExactly(
            SealedInterface.SealedClass::class,
            SealedInterface.SealedClass.SealedObjectInstance2::class,
            SealedInterface.SealedClass.SealedObjectInstance3::class,
            SealedInterface.SealedObjectInstance1::class,
        )
    }

    @Test fun all_sealed_object_instances() = testAll {
        JvmReflectionsKtTest::class.allSealedObjectInstances.shouldBeEmpty()
        SealedInterface::class.allSealedObjectInstances.shouldContainExactly(
            SealedInterface.SealedClass.SealedObjectInstance2,
            SealedInterface.SealedClass.SealedObjectInstance3,
            SealedInterface.SealedObjectInstance1,
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
