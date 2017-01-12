import no.novelda.*

j = new Jobs(
  factory: this,
  repo: 'xtXEP',
  revdeps: [['XTSW','xtX4Mapp']]
)
j.submoduleTrigger()
j.thinBuild()
