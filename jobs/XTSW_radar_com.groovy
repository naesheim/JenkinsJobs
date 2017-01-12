import no.novelda.*

// TODO: revdeps, xeethruutil and xtXethruInspiration
j = new Jobs(
    factory: this,
    repo: 'radar_com',
    buildStyle: ['unix': BuildStyle.DOCKER,
                 'win32': BuildStyle.GRADLE,
                 'osx': BuildStyle.GRADLE],
    machines: ["unix": "ubuntu-work1.novelda.no",
               "win32": "win7-1-build.novelda.no",
               'osx': 'osx'],
    revdeps: ['unix': ["gen_soteria_X2_system_tests",
                       "gen_soteria_radar_com",
                       "gen_soteria_fw_upgrade",
                       "gen_soteria_run_mc_examples"],
              'win32': ["gen_soteria_test_moduleconnector_matlab_integration"]]
);
j.submoduleTrigger()
j.thinBuild()
