package mb.spoofax.runtime.pie.builder.core

import mb.pie.runtime.core.*
import mb.vfs.path.PPath
import org.metaborg.core.project.IProject
import org.metaborg.core.project.ISimpleProjectService

typealias TransientProject = OutTransientEquatable<IProject, PPath>

class CoreLoadProj : Builder<PPath, TransientProject> {
  companion object {
    val id = "coreLoadProj"
  }

  override val id = Companion.id
  override fun BuildContext.build(input: PPath): TransientProject {
    val spoofax = Spx.spoofax()
    val projLoc = input.fileObject
    var project = spoofax.projectService.get(projLoc)
    if(project == null) {
      project = spoofax.injector.getInstance(ISimpleProjectService::class.java).create(projLoc)
    }
    project!!
    return OutTransientEquatableImpl(project, project.path)
  }
}

val IProject.path get() = this.location().pPath

fun BuildContext.loadProj(input: PPath) = requireOutput(CoreLoadProj::class.java, input).v