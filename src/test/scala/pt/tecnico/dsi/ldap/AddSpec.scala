package pt.tecnico.dsi.ldap

import org.ldaptive.LdapException
import org.scalatest.Assertions

import scala.concurrent.Future

class AddSpec extends UnitSpec {

  private val repetitions: Int = 3

  "addEntry" should "add entry successfully " in {
    val janeDoe: String = "Jane Doe"
    val number: String = "960000000"

    val assert = simpleLdap.addEntry(s"$cn=$janeDoe", Map(cn -> List(janeDoe), sn -> List(doe), telephoneNumber -> List(number),
      objectClass -> List(person))).flatMap { _ =>
      simpleLdap.search(s"$cn=$janeDoe", s"$cn=$janeDoe", size = 1).map { result =>
        result.headOption.map { entry =>
          entry.textValue(cn).value shouldBe janeDoe
          entry.textValue(sn).value shouldBe doe
          entry.textValue(telephoneNumber).value shouldBe number
        }.getOrElse(Assertions.fail)
      }
    }

    assert.onComplete { _ =>
      simpleLdap.deleteEntry(s"$cn=$janeDoe")
    }

    assert
  }

  it should "not fail when adding an already existing entry" in {
    val johnDoe = "John Doe"
    val number = "210000000"

    val assert = Future.sequence {
      (1 to repetitions).map { _ =>
        simpleLdap.addEntry(s"$cn=$johnDoe", Map(cn -> List(johnDoe), sn -> List(doe),
          telephoneNumber -> List(number), objectClass -> List(person)))
      }
    }.flatMap { _ =>
      simpleLdap.search(s"$cn=$johnDoe", s"$cn=$johnDoe", size = 1).map { result =>
        result.headOption.map { entry =>
          entry.textValue(cn) shouldBe Some(johnDoe)
          entry.textValue(sn) shouldBe Some(doe)
          entry.textValue(telephoneNumber) shouldBe Some(number)
        }.getOrElse(Assertions.fail)
      }
    }

    assert.onComplete { _ =>
      simpleLdap.deleteEntry(s"$cn=$johnDoe")
    }

    assert
  }

  it should "add new attributes" in {
    val anthonyDoe = "Anthony Doe"
    val number = "210000000"
    val description: String = "description"
    val descriptionValue: String = "Found in US"

    val assert = {
      simpleLdap.addEntry(s"$cn=$anthonyDoe", Map(cn -> List(anthonyDoe), sn -> List(doe),
        telephoneNumber -> List(number), description -> List(descriptionValue), objectClass -> List(person)))
    }.flatMap { _ =>
      simpleLdap.search(s"$cn=$anthonyDoe", s"$cn=$anthonyDoe", size = 1).map { result =>
        result.headOption.map { entry =>
          entry.textValue(cn) shouldBe Some(anthonyDoe)
          entry.textValue(sn) shouldBe Some(doe)
          entry.textValue(telephoneNumber) shouldBe Some(number)
          entry.textValue(description) shouldBe Some(descriptionValue)
        }.getOrElse(Assertions.fail)
      }
    }

    assert.onComplete { _ =>
      simpleLdap.deleteEntry(s"cn=$anthonyDoe")
    }

    assert
  }

  it should "fail when adding an empty entry" in {
    val ferdinandDoe = "Ferdinand Doe"
    recoverToSucceededIf[LdapException] {
      simpleLdap.addEntry(s"$cn=$ferdinandDoe")
    }
  }

  it should "fail when adding and entry without all required attributes" in {
    val samDoe = "Samuel Doe"
    recoverToSucceededIf[LdapException] {
      simpleLdap.addEntry(s"$cn=$samDoe", Map(cn -> List(samDoe)))
    }
  }

  // TODO multivalue attribute (((ter um valor repetido, ter mais valores, ter menos valores)

}
