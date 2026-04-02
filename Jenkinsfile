SYNGX_USER = "syngxdevops"
SYNGX_CONF = "/opt/syngx/conf/syngx.conf"
SYNGX_BIN  = "/opt/syngx/sbin/syngx"

def BALANCERS = ['10.109.36.102', '10.119.121.116', '10.109.3.194', '10.109.200.218', '10.119.110.225']
def BACKENDS  = ['10.119.121.44', '10.109.37.176', '30.88.36.249', '30.88.81.1']

pipeline {
    agent {
        node {
            label 'masterLin'
        }
    }

    parameters {
        choice(name: 'ACTION', choices: ['---', 'drain', 'swap', 'restore'],
            description: '''Выберите действие:<br>
<b>drain</b> - снять нагрузку со ВСЕХ бекенд-нод на всех балансировщиках, кроме выбранной в KEEP_BACKEND<br>
<b>swap</b> - снять нагрузку с выбранной в KEEP_BACKEND ноды и вернуть нагрузку на все остальные<br>
<b>restore</b> - вернуть нагрузку на ВСЕ ноды бекенда (штатный режим)''')

        choice(name: 'KEEP_BACKEND', choices: ['10.119.121.44', '10.109.37.176', '30.88.36.249', '30.88.81.1'],
            description: '''Нода бекенда для управления:<br>
<b>drain</b> - эта нода останется активной, остальные будут помечены как down<br>
<b>swap</b> - эта нода будет помечена как down, остальные будут возвращены в работу<br>
<b>restore</b> - параметр игнорируется, все ноды будут активны''')

        booleanParam(name: 'DRY_RUN', defaultValue: true,
            description: '''Режим проверки без применения изменений.<br>
Конфиг скачивается и модифицируется локально, но <b>не загружается</b> на балансировщик и <b>рестарт не выполняется</b>.''')
    }

    stages {

        stage('Validate') {
            steps {
                script {
                    if (params.ACTION == '---') {
                        error("Необходимо выбрать действие (ACTION)")
                    }
                    def dryLabel = params.DRY_RUN ? ' [DRY-RUN]' : ''
                    currentBuild.displayName = "#${BUILD_NUMBER} ${params.ACTION} [${params.KEEP_BACKEND}]${dryLabel}"
                    currentBuild.description = "Action: ${params.ACTION}, Backend: ${params.KEEP_BACKEND}, DRY_RUN: ${params.DRY_RUN}"

                    echo "=== Параметры запуска ==="
                    echo "ACTION:        ${params.ACTION}"
                    echo "KEEP_BACKEND:  ${params.KEEP_BACKEND}"
                    echo "DRY_RUN:       ${params.DRY_RUN}"
                    echo "Балансировщики: ${BALANCERS.join(', ')}"
                    echo "Бекенды:        ${BACKENDS.join(', ')}"
                    echo "========================="
                    if (params.DRY_RUN) {
                        echo "*** РЕЖИМ DRY-RUN: изменения НЕ будут применены на балансировщиках ***"
                    }
                }
            }
        }

        stage('Process Balancers') {
            steps {
                script {
                    withCredentials([[$class: 'VaultSignedSSHKeyCredentialBinding',
                        credentialsId: 'ssh_syngxdevops2',
                        privateKeyVar: 'SSH_PRIVATE_KEY',
                        passphraseVar: 'SSH_PRIVATE_KEY_PASS']]) {

                        sshagent([]) {
                            sh '''
                            set +x
                            echo -e "#!/bin/sh\necho \\$SSH_PRIVATE_KEY_PASS" > send_ps.sh
                            chmod +x ./send_ps.sh
                            DISPLAY=1 SSH_ASKPASS="./send_ps.sh" ssh-add $SSH_PRIVATE_KEY < /dev/null
                            '''

                            for (balancer in BALANCERS) {
                                processBalancer(balancer, params.ACTION, params.KEEP_BACKEND, BACKENDS, params.DRY_RUN)
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Все балансировщики успешно обработаны. Действие: ${params.ACTION}, бекенд: ${params.KEEP_BACKEND}"
        }
        failure {
            echo "ОШИБКА! Не все балансировщики были обработаны. Проверьте логи выше."
        }
        always {
            cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
    }
}

def processBalancer(String balancer, String action, String keepBackend, List backends, Boolean dryRun) {
    def sshOpts = "-o StrictHostKeyChecking=no -o CertificateFile=\$SIGNED_SSH_PUBLIC_KEY -i \$SSH_PRIVATE_KEY"
    def scpOpts = "-o StrictHostKeyChecking=no -i \$SSH_PRIVATE_KEY"
    def remote  = "${SYNGX_USER}@${balancer}"
    def localConf = "syngx_${balancer}.conf"
    def modeLabel = dryRun ? ' [DRY-RUN]' : ''

    echo "========== Обработка балансировщика: ${balancer}${modeLabel} =========="

    // 1. Скачать текущий конфиг с ноды балансировщика
    sh "scp ${scpOpts} ${remote}:${SYNGX_CONF} ${localConf}"

    // 2. Показать текущее состояние upstream (бекенд-серверы)
    echo "--- Текущее состояние upstream на ${balancer} ---"
    sh "grep -nE 'server.*(${backends.join('|')})' ${localConf} || echo 'Бекенд-серверы не найдены в конфиге'"

    // 3. Модифицировать конфиг локально (добавить/убрать down)
    modifyConfig(localConf, action, keepBackend, backends)

    // 4. Показать изменённое состояние upstream
    echo "--- Состояние upstream после изменений для ${balancer} ---"
    sh "grep -nE 'server.*(${backends.join('|')})' ${localConf} || echo 'Бекенд-серверы не найдены в конфиге'"

    if (dryRun) {
        echo "[DRY-RUN] Пропуск: загрузка конфига, проверка и рестарт на ${balancer}"
        echo "========== Балансировщик ${balancer} обработан [DRY-RUN] =========="
        return
    }

    // 5. Создать бекап конфига на удалённом сервере
    sh "ssh ${sshOpts} ${remote} 'cp ${SYNGX_CONF} ${SYNGX_CONF}.bak'"

    // 6. Загрузить изменённый конфиг обратно на балансировщик
    sh "scp ${scpOpts} ${localConf} ${remote}:${SYNGX_CONF}"

    // 7. Проверить валидность конфига на балансировщике
    def testResult = sh(script: "ssh ${sshOpts} ${remote} 'sudo ${SYNGX_BIN} -t'", returnStatus: true)

    if (testResult != 0) {
        echo "ОШИБКА: Проверка конфига не пройдена на ${balancer}! Восстанавливаем бекап..."
        sh "ssh ${sshOpts} ${remote} 'cp ${SYNGX_CONF}.bak ${SYNGX_CONF}'"
        error("Проверка конфигурации syngx не пройдена на ${balancer}. Бекап восстановлен. Обработка остановлена.")
    }

    // 8. Рестарт syngx после успешной проверки
    sh "ssh ${sshOpts} ${remote} 'sudo systemctl restart syngx'"

    echo "========== Балансировщик ${balancer} обработан успешно =========="
}

def modifyConfig(String configFile, String action, String keepBackend, List backends) {
    def sedExpressions = []

    // Шаг 1: Нормализация - убрать все маркеры 'down' с бекенд-серверов
    // (чтобы избежать дублирования 'down down' при повторных запусках)
    for (backend in backends) {
        sedExpressions.add("-e '/^[[:space:]]*server.*${backend}:/s/ down//g'")
    }

    // Шаг 2: Применить действие
    switch (action) {
        case 'drain':
            // Пометить ВСЕ бекенды как down, КРОМЕ keepBackend
            for (backend in backends) {
                if (backend != keepBackend) {
                    sedExpressions.add("-e '/^[[:space:]]*server.*${backend}:/s/;/ down;/'")
                }
            }
            break
        case 'swap':
            // Пометить keepBackend как down, остальные уже активны после нормализации
            sedExpressions.add("-e '/^[[:space:]]*server.*${keepBackend}:/s/;/ down;/'")
            break
        case 'restore':
            // Нормализация на шаге 1 уже убрала все 'down' - все бекенды активны
            break
    }

    sh "sed -i ${sedExpressions.join(' ')} ${configFile}"
}
