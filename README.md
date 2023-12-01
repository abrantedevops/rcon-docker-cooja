<h1 align="center">Utilização do Docker para o provisionamento do simulador Cooja no Contiki-NG</h1>

<p align="center">
  <a href="#-sobre-o-projeto">Sobre o projeto</a> •
  <a href="#-como-executar-o-projeto">Requisitos Gerais</a> •
  <a href="#-prov">Cooja Simulator</a> •
  <a href="#-tecnologias">Cenários Estudados</a> •
</p>
<hr>
<h2 id="-sobre-o-projeto">Sobre o projeto</h2>

<p align="justify">Este projeto tem como objetivo a utilização do Docker para o provisionamento do simulador Cooja no Contiki-NG. O Contiki-NG é um sistema operacional de código aberto para a Internet das Coisas. O Contiki-NG é uma continuação do Contiki OS, um sistema operacional de código aberto para redes de sensores sem fio, sendo desenvolvido por uma equipe mundial de desenvolvedores independentes, com o apoio de empresas como a Atmel, Cisco, Texas Instruments, Zolertia, etc.</p>

<h2 id="-como-executar-o-projeto">Requisitos Gerais</h2>

<p align="justify">

- Toda a implementação foi realizada na distribuição Ubuntu 20.04. LTS Focal Fossa;
- Ter o Docker instalado para os serviços do MySQL e Grafana, para isso execute o script install_docker.sh</p>

```bash
$ chmod +x install_docker.sh && ./install_docker.sh
```


<h2 id="-prov">Cooja Simulator</h2>

<p align="justify">Diferente da simulação que envolve o cenário do MQTT-SN (Disponível na Branch develop deste repositório), aqui o Cooja é iniciado diretamente no host.</p>

<p align="justify">Instale as dependências abaixo juntamente com os comandos. Após a compilação, a tela padrão do Cooja será aberta.</p>

```bash
$ sudo apt update -y ; sudo apt install make gcc mosquitto ant maven default-jre default-jdk -y
$ git clone -b IoT_proj https://github.com/abrantedevops/rcon-docker-cooja.git
$ cd ~/rcon-docker-cooja ; git submodule update --init --recursive ; cd tools/cooja/ ; sudo ant run
```

<h2 id="-tecnologias">Cenários Estudados</h2>

<h3>Simulação 1: Funcionamento de uma Rede RPL com o protocolo MQTT-SN.</h3>

<p align="justify">Confira todos os passos para a execução da simulação no arquivo README.md da Branch develop deste repositório.</p>

<h3>Simulação 2: Rede IoT com banco de dados MySQL e Grafana</h3>

<p align="justify">A ideia geral desta prática consiste na integração do Cooja com o Docker através da simulação de uma rede de dispositivos IoT projetada para coletar dados do ambiente físico através da utilização de sensores e atuadores. Na sequência através do Docker Compose é instanciado um banco de dados MySQL e o Grafana configurados para armazenar e exibir os dados registrados na simulação por meio de um dashboard. A metade dos dispositivos na rede usa o protocolo CoAP para expor seus recursos, enquanto a outra metade utiliza o protocolo MQTT. Para mais detalhes: https://github.com/federicominniti/SmartWellness </p>

<p>I) Configuração do MySQL</p>

```bash
$ cd ~/rcon-docker-cooja/Smtw ; sudo docker-compose up -d ; sudo docker exec -it mysql bash
$ mysql -u root -p smartwellness < /docker-entrypoint-initdb.d/smartwellness_db.sql
$ enter password: PASSWORD
$ mysql -u root -pPASSWORD
> USE smartwellness;
> GRANT ALL PRIVILEGES ON smartwellness.* TO 'grafana'@'%';
> GRANT SELECT ON smartwellness.* TO 'grafana';
> FLUSH PRIVILEGES;
> SELECT * FROM DataSamples;
> source /docker-entrypoint-initdb.d/script.sql;
> exit

# Teste para acessar o db através do host
# $ mysql -u grafana -h 127.0.0.1 -pPWORD
```


<p align="justify">II) Abra mais dois terminais e execute os seguintes passos:</p>

```bash
Terminal 1: $ mosquitto -v
Terminal 2: $ cd ~/rcon-docker-cooja/Smtw/SmartWellnessCollector ; mvn clean install ; cd target ; java -jar SmartWellnessCollector-1.0-SNAPSHOT.jar
```

<p align="justify">III) Com o simulador aberto na tela inicial, abra o cenário em: File > Open Simulation > Open and Reconfigure > Browser. Na janela que abrir selecione a simulação "SmartWellness.csc" no ficheiro: ~/rcon-docker-cooja/Smtw/cooja. Nesse momento será carregado na tela uma janela específica para os nós de toda a simulação, mantenha a opção padrão "org.contikios.cooja.contikimote.ContikiMoteType" e prossiga. Em seguida uma tela de criação do nó é exibida na tela, em que o primeiro nó é o roteador de borda RPL cujo arquivo "border-router.c" precisa ser importado, para isso informe no campo do ficheiro o caminho: ~/rcon-docker-cooja/Smtw/rpl-border-router/border-router.c. Nesse momento clique em "Compile" e depois em "Create". Para todos os outros nós da simulação mantenha a opção padrão "org.contikios.cooja.contikimote.ContikiMoteType", informe o caminho do arquivo, compile e crie. Os caminhos dos arquivos a serem importados são:</p>

<p align="justify"> 

  - Nó 1: ~/rcon-docker-cooja/Smtw/rpl-border-router/border-router.c
  - Nó 2: ~/rcon-docker-cooja/Smtw/CoAP-network/air-conditioning/ac_CoAP_server.c
  - Nó 3: ~/rcon-docker-cooja/Smtw/CoAP-network/light-regulation/light-regulation_CoAP-server.c
  - Nó 4: ~/rcon-docker-cooja/Smtw/MQTT-network/humidifier/humidifier.c
  - Nó 5: ~/rcon-docker-cooja/Smtw/MQTT-network/access/access-control.c
  - Nó 6: ~/rcon-docker-cooja/Smtw/CoAP-network/water-quality/water_quality_CoAP_server.c
  - Nó 7: ~/rcon-docker-cooja/Smtw/MQTT-network/chlorine/chlorine-control.c

</p>

<p align="justify">IV) Inicie a simulação em "Start" localizado no quadro "Simulation control". e ative o tunelamento com o comando abaixo em um terceiro terminal</p>

```bash
Terminal 3: $ cd ~/rcon-docker-cooja/Smtw/rpl-border-router ; make TARGET=zoul connect-router-cooja
```

<p align="justify">Aguarde a convergência dos endpoints e observe a dinâmica de eventos por cada um dos nós no simulador, nesse momento a coleta de informações está sendo realizada e o banco será populado com os valores dos sensores. Observe o comportamento do broker mosquitto (terminal 1) e do coletor (terminal 2).</p>


<p>V) Configuração do Grafana</p>

<p align="justify"> Acesse http://localhost:3000 com a credencial admin/admin e digite mysql no campo de busca e selecione o banco, em seguida na tela de configuração coloque as seguintes variáveis:</p>

<p align="justify"> 

- Host: ip_container_mysql:3306
- Database name: smartwellness
- Username: grafana
- Password: PWORD
- Marque Skip TLS Verification
- Em seguida, clique em "Save & test".

</p>

<p align="justify">Agora com o banco conectado, basta importar um modelo de dashboard do grafana que está localizado em ~/rcon-docker-cooja/Smtw/grafana, na tela de opções determine um nome, e no UID user: -T1TqUenk e depois clique em "Import". Nesse momento irá abrir a tela geral do Dashboard com todos os gráficos, selecione o que desejar, e clique em editar. Na tela seguinte clique no botão "Run query" para que o grafana importe os valores do banco.</p>

<br>

<p align="center">
  <p align="center">Simulação 2 - Funcionamento da Rede IoT com banco de dados MySQL e Grafana</p>
  <img src="img/fct-g.gif" alt="animated" />
</p>

<br>


