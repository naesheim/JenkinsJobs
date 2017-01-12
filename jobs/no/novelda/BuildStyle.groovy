package no.novelda

public enum BuildStyle {
    DOCKER('docker', 'starts ./docker/dockerRun.sh -P release=<bool>'),
    GRADLE('gradle', 'starts ./gradlew[.bat] -i publish -P release=<bool>'),
    NOOP('noop', 'does nothing')

    final String id
    final String desc

    private BuildStyle(String id, String desc) {
        this.id = id
        this.desc = desc
    }
}
