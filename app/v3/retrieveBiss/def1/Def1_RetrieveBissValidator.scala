/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.retrieveBiss.def1

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYear}
import v3.retrieveBiss.controllers.validators.resolvers.ResolveTypeOfBusiness
import api.models.domain.TaxYear
import v3.retrieveBiss.model.domain.TypeOfBusiness.*
import api.models.errors.{MtdError, RuleTaxYearNotSupportedError, TypeOfBusinessFormatError}
import cats.data.Validated
import cats.data.Validated.*
import cats.implicits.*
import v3.retrieveBiss.model.domain.TypeOfBusiness
import v3.retrieveBiss.model.request.{Def1_RetrieveBissRequestData, RetrieveBissRequestData}

import javax.inject.Singleton

@Singleton
class Def1_RetrieveBissValidator(nino: String, typeOfBusiness: String, taxYear: String, businessId: String)
    extends Validator[RetrieveBissRequestData] {

  private val foreignPropertyMinimumTaxYear = TaxYear.fromMtd("2019-20")

  def validate: Validated[Seq[MtdError], RetrieveBissRequestData] =
    (
      ResolveNino(nino),
      ResolveTypeOfBusiness(typeOfBusiness),
      ResolveTaxYear(taxYear),
      ResolveBusinessId(businessId)
    ).mapN(Def1_RetrieveBissRequestData.apply) andThen validateWithTypeOfBusinessAndTaxYear

  private def validateWithTypeOfBusinessAndTaxYear(parsed: RetrieveBissRequestData): Validated[Seq[MtdError], RetrieveBissRequestData] = {
    (parsed.typeOfBusiness, parsed.taxYear.year) match {
      case (`uk-property-fhl` | `foreign-property-fhl-eea`, year) if year >= TypeOfBusiness.fhlPropertyMinimumTaxYear.year =>
        Invalid(List(TypeOfBusinessFormatError))

      case (`foreign-property-fhl-eea` | `foreign-property`, year) if year < foreignPropertyMinimumTaxYear.year =>
        Invalid(List(RuleTaxYearNotSupportedError))

      case (`uk-property` | `uk-property-fhl` | `self-employment`, year) if year < TaxYear.minimumTaxYear.year =>
        Invalid(List(RuleTaxYearNotSupportedError))

      case _ => Valid(parsed)
    }
  }

}
