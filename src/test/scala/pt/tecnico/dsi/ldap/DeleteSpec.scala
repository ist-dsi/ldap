package pt.tecnico.dsi.ldap

class DeleteSpec extends UnitSpec {

  "deleteEntry" should "delete an entry successfully" in {
    val removableDoe: String = s"Removable $doe"
    for {
      _ <- simpleLdap.addEntry(s"$cn=$removableDoe", Map(cn -> List(removableDoe), sn -> List(doe),objectClass -> List(person)))
      _ <- simpleLdap.deleteEntry(s"$cn=$removableDoe")
      result <- simpleLdap.searchAll(s"$cn=$removableDoe", s"$cn=$removableDoe")
    } yield result.isEmpty shouldBe true
  }

  it should "not fail when deleting a non existant entry" in {
    val johnDoe = s"John $doe"

    for {
      _ <- simpleLdap.deleteEntry(s"$cn=$johnDoe")
      result <- simpleLdap.searchAll(s"$cn=$johnDoe", s"$cn=$johnDoe")
    } yield result.isEmpty shouldBe true
  }

}
