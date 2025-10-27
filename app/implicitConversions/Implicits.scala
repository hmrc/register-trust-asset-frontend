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

package implicitConversions

object Implicits {

  implicit class StringImplicits(str: String) {
    def uncapitalize: String = str.split(' ').foldLeft("") { (acc, word) =>
      def uncapitalizeWord = s"${word.head.toLower}${word.tail}"
      if (acc.isEmpty) {
        uncapitalizeWord
      } else {
        s"$acc $uncapitalizeWord"
      }
    }

    def lowercaseFirstWord: String = str match {
      case "" => ""
      case s => s.head.toLower.toString + s.tail
    }
  }

  implicit class ListImplicits[T](list: List[T]) {
    def asSomeIf(condition: Boolean): Option[List[T]] = list match {
      case _ if !condition => None
      case _               => Some(list)
    }
  }
}
