web: target/start -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS}
scheduledScrapeGS: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobGS
scheduledScrapeGM: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobGM
scheduledScrapeGG: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobGG
scheduledScrapeSt: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobSt
scheduledScrapeDl: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobDl
scheduledScrapeStDLC: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobStDLC
scheduledScrapeStPk: java -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} ${JAVA_OPTS} -cp "target/staged/*" jobs.ScrapeJobStPk