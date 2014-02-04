package com.guidewire.tarot

import akka.actor._
import spray.util.SprayActorLogging
import org.jclouds.ContextBuilder
import com.google.common.collect.ImmutableSet
import com.google.inject.Module
import org.jclouds.compute.ComputeServiceContext
import org.jclouds.rest.RestContext
import org.jclouds.openstack.nova.v2_0.{NovaApiMetadata, NovaAsyncApi, NovaApi}
import org.jclouds.openstack.nova.v2_0.domain.Server
import java.util.UUID
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions

object MachineProvisionActor {
  case object Start
  case object Stop

  def apply(engine: ActorRef, name:String)(implicit system: ActorSystem) =
    system.actorOf(Props(new MachineProvisionActor(engine)), name)
}

class MachineProvisionActor(engine:ActorRef) extends Actor with SprayActorLogging {
  import EngineActor._
  import MachineProvisionActor._

  val identity = "tarot:tarot" // tenantName:userName
  val password = "tarot"       // demo account uses ADMIN_PASSWORD too
  val keystone_endpoint = "http://10.220.12.21:5000/v2.0"

  override def preStart() {
    self ! Start
  }

  def receive = {
    case Start =>
      engine ! RegisterFinishedListener(self)

    case Stop =>
      engine ! UnregisterFinishedListener(self)

    case FinishedEvent(results) =>
      for ((id, change) <- results.decision.delta)
        delta(id, change)
  }

  def addMachines(count:Int) = {
    val api = JCloudsUtil.createNovaApi(keystone_endpoint, identity, password)

    try {
      val zone = api.getConfiguredZones().iterator().next()
      val server_api = api.getServerApiForZone(zone)
      val options = CreateServerOptions.Builder
        .keyPairName("dhoyt-t3500")
        .securityGroupNames("default")

      val image_ref = "99ff3483-bc36-4531-8df0-e754995218e1" //ubuntu-13.04-server-x86_64-clean
      val flavor_ref = "0430abfb-df99-40af-be8d-228b49ce8423" //tarot.1

      var remaining = count
      while(remaining > 0) {

        var rand_uuid = UUID.randomUUID().toString()
        rand_uuid = rand_uuid.substring(rand_uuid.length - 12)

        val name = s"tarot-$rand_uuid"

        log info s"Attempting to create new tarot machine named $name"

        val created = server_api.create(name, image_ref, flavor_ref, options)

        log info s"New tarot server created named $name (${created.getId()})"

        remaining -= 1
      }
    } catch {
      case t:Throwable => log warning s"${t.getMessage}"
    } finally {
      api.close()
    }
  }

  def removeMachines(count:Int) = {
    val api = JCloudsUtil.createNovaApi(keystone_endpoint, identity, password)

    var remaining = count

    try {
      val zone = api.getConfiguredZones().iterator().next()
      val server_api = api.getServerApiForZone(zone)

      val server_list = server_api.listInDetail().concat().iterator()
      while(server_list.hasNext() && remaining > 0) {
        val machine = server_list.next()

        val name = machine.getName()
        val in_use = JCloudsUtil.isInUse(machine)

        if (in_use) {
          try {
            log info s"Attempting to delete tarot machine $name (${machine.getId()})"
            server_api.delete(machine.getId())
            remaining -= 1
          } catch {
            case t:Throwable => log warning s"${t.getMessage()}"
          }
        }
      }
    } catch {
      case t:Throwable => log warning s"${t.getMessage}"
    } finally {
      api.close()
    }
  }

  def delta(id:UID[_], change:Int) = {
    //For demo purposes, only 1 type of machine is added/removed.
    change match {
      case x if change < 0 => removeMachines(math.abs(x))
      case x if change > 0 => addMachines(x)
      case _ => log info s"No work to do"
    }
  }
}
