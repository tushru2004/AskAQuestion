package com.example

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Future
import scala.collection.immutable
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.example.JsonFormats._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

final case class ApiResponse(answer: String)
final case class Question(question: String)

//#user-case-classes

object QuestionRegistry {
  // actor protocol
  sealed trait Command
  final case class AskQuestion(quest: Question, replyTo: ActorRef[PostPdfResponse]) extends Command
  final case class PostPdfResponse(answer: String)

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case AskQuestion(quest, replyTo) =>
        implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "SingleRequest")
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext: ExecutionContextExecutor = system.executionContext
        val jsonBody = s"""{"question": "${quest.question}"}"""
        // Create the POST request
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = "http://localhost:3557/pdf/ask", // Replace with your endpoint
          entity = HttpEntity(
            ContentTypes.`application/json`,
            jsonBody
          )
        )
        val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
        // Define JSON format for the API response
        responseFuture.flatMap { response =>
          if (response.status == StatusCodes.OK) {
            Unmarshal(response.entity).to[ApiResponse].map { apiResponse =>
              // Send the actual answer string back
              replyTo ! PostPdfResponse(apiResponse.answer)
            }
          } else {
            system.log.error(s"Request failed with status code: ${response.status}")
            Future.successful(replyTo ! PostPdfResponse(s"Error: Request failed with status code: ${response.status}"))
          }
        }.recover {
          case ex =>
            system.log.error("Error processing response", ex)
            replyTo ! PostPdfResponse(s"Error: ${ex.getMessage}")
        }

        responseFuture.onComplete(_ => system.terminate())
        Behaviors.same
    }
}
