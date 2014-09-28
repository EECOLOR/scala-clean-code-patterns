package processes.monadTransformers.scalaz

import play.api.mvc.AnyContent
import play.api.mvc.Request
import processes.PatchAssignment
import processes.monadTransformers.Enhancements

object Enhanced extends PatchAssignment with ScalazMachinery with Enhancements {

  def handlePatchRequest(id: String, request: Request[AnyContent]) = {
    val patchProgram =
      for {
        json <- services.parseJson(request) ifEmpty results.badRequest
        profile <- services.jsonToProfile(json) ifError results.unprocessableEntity
        existingProfile <- services.getProfileById(id) ifEmpty results.notFound(id)
        mergedProfile <- services.mergeProfile(existingProfile, profile).toResult
        _ <- services.updateProfile(id, mergedProfile).toResult
      } yield results.noContent

    patchProgram.run
      .map(_.merge)
      .recover(PartialFunction(results.internalServerError))
  }
}
