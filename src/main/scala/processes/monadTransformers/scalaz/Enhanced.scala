package processes.monadTransformers.scalaz

import play.api.mvc.AnyContent
import play.api.mvc.Request
import processes.PatchAssignment
import processes.monadTransformers.Enhancements
import processes.Services

class Enhanced(services:Services) extends PatchAssignment with Machinery with Enhancements {

  def handlePatchRequest(id: String, request: Request[AnyContent]) = {
    val patchProgram =
      for {
        json <- services.parseJson(request) ifEmpty results.badRequest
        profile <- services.jsonToProfile(json) ifError results.unprocessableEntity
        existingProfile <- services.getProfileById(id) ifEmpty results.notFound(id)
        mergedProfile <- services.mergeProfile(existingProfile, profile).toResult
        _ <- services.updateProfile(id, mergedProfile).toResult
      } yield results.noContent

    patchProgram.run.map(_.merge)
  }
}
