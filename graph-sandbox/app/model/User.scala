package model

import scala.util.{Try, Failure, Success}
import java.util.Hashtable
import javax.naming.{NamingEnumeration, Context}
import javax.naming.directory.{SearchResult, SearchControls}
import com.guidewire.tarot.LDAPSocketFactory
import javax.naming.ldap.{InitialLdapContext, LdapContext}
import play.api.mvc.RequestHeader

case class User(account:String, displayName:String, email:String)

object User {

  def authenticate(account:String, password:String): Option[Boolean] =
    MyAuthentication.authenticate(account, password).toOption

  def lookup(account:String): Option[App.View.Account.Details] =
    MyAuthentication.lookup(account).toOption

  trait Authenticator {
    def authenticate(account:String, password:String):Try[Boolean]
  }

  trait AccountLocator {
    def lookup(account:String):Try[App.View.Account.Details]
  }

  private[this] object MyAuthentication extends Authenticator with AccountLocator {
    val LDAP_SERVER      = "" // ldaps://<host>:<port
    val DOMAIN           = "" // foo.com
    val SEARCH_BASE      = "" // DC=foo,DC=com
    val ACCOUNT_DETAILS  = "" // (&(objectCategory=Person)(sAMAccountName=%s)) ?

    val BIND_DN          = "" //cn=ldap,ou=<system accounts>,ou=<users>,dc=foo,dc=com"
    val BIND_DN_PASSWORD = "" //<password>

    val ATTRIBUTE_FOR_DISPLAY_NAME = "displayName"
    val ATTRIBUTE_FOR_EMAIL        = "mail"

    def authenticate(account:String, password:String):Try[Boolean] = {
      val env = new Hashtable[String, String]()
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      env.put(Context.SECURITY_PROTOCOL, "ssl")
      env.put(Context.SECURITY_AUTHENTICATION, "simple")
      env.put(Context.SECURITY_PRINCIPAL, s"$account@$DOMAIN")
      env.put(Context.SECURITY_CREDENTIALS, password)
      env.put(Context.PROVIDER_URL, LDAP_SERVER)
      env.put("java.naming.ldap.factory.socket", LDAPSocketFactory.factoryName)

      var ctx:LdapContext = null

      try {
        ctx = new InitialLdapContext(env, null)
        Success(true)
      } catch {
        case t:Throwable => Failure(t)
      } finally {
        if (ctx != null) {
          ctx.close()
        }
      }
    }

    def lookup(account:String):Try[App.View.Account.Details] = {
      val env = new Hashtable[String, String]()
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      env.put(Context.SECURITY_PROTOCOL, "ssl")
      env.put(Context.SECURITY_AUTHENTICATION, "simple")
      env.put(Context.SECURITY_PRINCIPAL, BIND_DN)
      env.put(Context.SECURITY_CREDENTIALS, BIND_DN_PASSWORD)
      env.put(Context.PROVIDER_URL, LDAP_SERVER)
      env.put("java.naming.ldap.factory.socket", LDAPSocketFactory.factoryName)

      var ctx:LdapContext = null
      var results:NamingEnumeration[SearchResult] = null

      try {
        ctx = new InitialLdapContext(env, null)

        val ctrl = new SearchControls()
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE)

        results = ctx.search(SEARCH_BASE, ACCOUNT_DETAILS.format(account), ctrl)

        val result = results.next()
        val attributes = result.getAttributes()

        val display_name = attributes.get(ATTRIBUTE_FOR_DISPLAY_NAME).get().toString
        val email = attributes.get(ATTRIBUTE_FOR_EMAIL).get().toString

        //val groups = attributes.get("memberOf").getAll
        //while(groups.hasMoreElements) {
        //  val group = groups.nextElement().asInstanceOf[String]
        //
        //  println("G: " + group.toString)
        //}

        Success(App.View.Account.Details(account, display_name, email))
      } catch {
        case t:Throwable =>
          Failure(t)
      } finally {
        if (results != null) {
          results.close()
        }
      }
    }
  }
}
