package pt.tecnico.dsi.ldap

import java.io.{FileOutputStream, ObjectOutputStream}

import org.scalatest.Assertions

class SerializationSpec extends UnitSpec {
  "Serialization" should "be sucessfull" in {
    val francisDoe: String = "Francis Doe"
    val number: String = "960000000"

    val assert = simpleLdap.addEntry(s"$cn=$francisDoe", Map(cn -> List(francisDoe), sn -> List("Doe"),
      telephoneNumber -> List(number), objectClass -> List(person))).flatMap { _ =>

      simpleLdap.search(s"$cn=$francisDoe", s"$cn=$francisDoe", size = 1).map { result =>
        result.headOption.map { entry =>
          val os = new ObjectOutputStream(new FileOutputStream("/tmp/example.dat"))
          os.writeObject(entry)
          os.close()

          entry.textValue(cn).value shouldBe francisDoe
          entry.textValue(sn).value shouldBe "Doe"
          entry.textValue(telephoneNumber).value shouldBe number
        }.getOrElse(Assertions.fail)
      }
    }

    assert.onComplete { _ =>
      simpleLdap.deleteEntry(s"$cn=$francisDoe")
    }

    assert
  }
}
