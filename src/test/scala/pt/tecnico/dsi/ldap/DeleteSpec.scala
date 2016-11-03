package pt.tecnico.dsi.ldap

class DeleteSpec extends UnitSpec {

  "deleteEntry" should "delete an entry successfully" in {
    val removableDoe: String = s"Removable $doe"

    simpleLdap.addEntry(s"$cn=$removableDoe", Map(cn -> List(removableDoe), sn -> List(doe),
      objectClass -> List(person))).flatMap { _ => // Setup (add)
      simpleLdap.deleteEntry(s"$cn=$removableDoe").flatMap { _ => // Action (delete)
        simpleLdap.searchAll(s"$cn=$removableDoe", s"$cn=$removableDoe").map { result => // Assert (search)
          result.isEmpty shouldBe true
        }
      }
    }
  }

  it should "not fail when deleting a non existant entry" in {
    val mantorrasDoe = s"John $doe"

    simpleLdap.deleteEntry(s"$cn=$mantorrasDoe").flatMap { _ =>
      simpleLdap.searchAll(s"$cn=$mantorrasDoe", s"$cn=$mantorrasDoe").map { result =>
        result.isEmpty shouldBe true
      }
    }
  }

}
