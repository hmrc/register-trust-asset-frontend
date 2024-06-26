/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import base.SpecBase
import generators.Generators
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.QuestionPage
import play.api.http.Writeable
import play.api.libs.json.Writes
import play.api.mvc.Request
import play.api.test.Helpers._
import views.html.PageNotFoundView

trait IndexValidation extends SpecBase with ScalaCheckPropertyChecks with Generators {

  def validateIndex[A, B](
    generator: Gen[A],
    createPage: Int => QuestionPage[A],
    requestForIndex: Int => Request[B]
  )(implicit writes: Writes[A], writeable: Writeable[B]): Unit =
    "return not found if a given index is out of bounds" in {
      implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
        minSuccessful = 2
      )

      val gen = for {
        answers <- Gen.listOf(generator).map(_.zipWithIndex)
        index   <- Gen.oneOf(
                     Gen.chooseNum(answers.size + 1, answers.size + 100),
                     Gen.chooseNum(-100, -1)
                   )
      } yield (answers, index)

      forAll(gen) { case (answers, index) =>
        val userAnswers = answers.foldLeft(emptyUserAnswers) { case (uA, (answer, i)) =>
          uA.set(createPage(i), answer).success.value
        }

        val application = applicationBuilder(Some(userAnswers))
          .build()

        val request = requestForIndex(index)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PageNotFoundView]

        val applyView = view.apply()(request, messages)

        status(result) mustEqual NOT_FOUND

        contentAsString(result) mustEqual applyView.toString

        application.stop()
      }
    }
}
