package org.apache.maven.project.builder.profile;


public class ProfileUri {

    public static class Profiles
    {
        public static String xUri = "http://apache.org/maven/project/profiles#collection";

        public static class Profile
        {
            public static String xUri = "http://apache.org/maven/project/profiles#collection/profile";

            public static String id = "http://apache.org/maven/project/profiles#collection/profile/id";

            public static class Activation
            {
                public static String xUri = "http://apache.org/maven/profiles#collection/profile/activation";

                public static String activeByDefault =
                    "http://apache.org/maven/profiles#collection/profile/activation/activeByDefault";

                public static String jdk = "http://apache.org/maven/profiles#collection/profile/activation/jdk";

                public static class Os
                {
                    public static String xUri =
                        "http://apache.org/maven/profiles#collection/profile/activation/os";

                    public static String name =
                        "http://apache.org/maven/profiles#collection/profile/activation/os/name";

                    public static String family =
                        "http://apache.org/maven/profiles#collection/profile/activation/os/family";

                    public static String arch =
                        "http://apache.org/maven/profiles#collection/profile/activation/os/arch";

                    public static String version =
                        "http://apache.org/maven/profiles#collection/profile/activation/os/version";
                }

                public static class Property
                {
                    public static String xUri =
                        "http://apache.org/maven/profiles#collection/profile/activation/property";

                    public static String name =
                        "http://apache.org/maven/profiles#collection/profile/activation/property/name";

                    public static String value =
                        "http://apache.org/maven/profiles#collection/profile/activation/property/value";
                }

                public static class File
                {
                    public static String xUri =
                        "http://apache.org/maven/profiles#collection/profile/activation/file";

                    public static String missing =
                        "http://apache.org/maven/profiles#collection/profile/activation/file/missing";

                    public static String exists =
                        "http://apache.org/maven/profiles#collection/profile/activation/file/exists";
                }
            }

            public static class Repositories
            {
                public static String xUri = "http://apache.org/maven/profiles#collection/profile/repositories";

                public static class Repository
                {
                    public static String xUri =
                        "http://apache.org/maven/profiles#collection/profile/repositories/repository";

                    public static class Releases
                    {
                        public static String xUri =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/releases";

                        public static String enabled =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/releases/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/releases/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/releases/checksumPolicy";
                    }

                    public static class Snapshots
                    {
                        public static String xUri =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/snapshots";

                        public static String enabled =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/snapshots/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/snapshots/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/profiles#collection/profile/repositories/repository/snapshots/checksumPolicy";
                    }

                    public static String id =
                        "http://apache.org/maven/profiles#collection/profile/repositories/repository/id";

                    public static String name =
                        "http://apache.org/maven/profiles#collection/profile/repositories/repository/name";

                    public static String url =
                        "http://apache.org/maven/profiles#collection/profile/repositories/repository/url";

                    public static String layout =
                        "http://apache.org/maven/profiles#collection/profile/repositories/repository/layout";
                }
            }

            public static class PluginRepositories
            {
                public static String xUri =
                    "http://apache.org/maven/profiles#collection/profile/pluginRepositories";

                public static class PluginRepository
                {
                    public static String xUri =
                        "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository";

                    public static class Releases
                    {
                        public static String xUri =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/releases";

                        public static String enabled =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/releases/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/releases/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/releases/checksumPolicy";
                    }

                    public static class Snapshots
                    {
                        public static String xUri =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots";

                        public static String enabled =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/checksumPolicy";
                    }

                    public static String id =
                        "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/id";

                    public static String name =
                        "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/name";

                    public static String url =
                        "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/url";

                    public static String layout =
                        "http://apache.org/maven/profiles#collection/profile/pluginRepositories/pluginRepository/layout";
                }
            }

            public static String properties = "http://apache.org/maven/profiles#collection/profile/properties";
        }
    }
}
