package com.pdf

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.pdf.QuestionRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import JsonFormats._

class PdfRoutes(userRegistry: ActorRef[QuestionRegistry.Command])(implicit val system: ActorSystem[_]) {
  
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("ask-a-question.routes.ask-timeout"))

  def askQuestion(question: Question): Future[PostPdfResponse] =
    userRegistry.ask(AskQuestion(question, _))

  def questionRoutes:Route =
    pathPrefix("question") {
      pathPrefix("ask") {
        concat(
          askQuestionRoute
        )
      }
    }

  private def askQuestionRoute: Route =
    pathEnd {
      post {
        entity(as[Question]) { question =>
          system.log.info(s"Received question: ${question.question}")
          onSuccess(askQuestion(question)) { performed =>
            system.log.info(s"Action performed: ${performed}")
            complete((StatusCodes.OK, performed.answer))
          }
        }
      }
    }
}
