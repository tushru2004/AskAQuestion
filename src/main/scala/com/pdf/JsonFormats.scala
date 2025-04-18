package com.pdf

import com.pdf.QuestionRegistry.PostPdfResponse

//#json-formats
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  import DefaultJsonProtocol._

  implicit val questionJsonFormat: RootJsonFormat[Question] = jsonFormat1(Question.apply)
  implicit val pdfResponseJsonFormat: RootJsonFormat[PostPdfResponse] = jsonFormat1(PostPdfResponse.apply)
  implicit val apiResponseJsonFormat: RootJsonFormat[ApiResponse] = jsonFormat1(ApiResponse.apply)
}
//#json-formats
