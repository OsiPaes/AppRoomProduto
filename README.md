# AppRoomProduto: Cadastro de Produtos com Room SQLite

**Aluno(a):** Osi Paes Junior
**RA:** 000000
**Disciplina:** Desenvolvimento para Dispositivos Móveis
**Professor(a):** Alessandro Ferreira

## Sobre o projeto
Aplicativo Android em Kotlin que cadastra, lista, busca, atualiza e exclui produtos
usando persistência local com Room (SQLite). Os dados continuam salvos mesmo depois
de fechar o app.

## Tecnologias
- Kotlin
- Room 2.7.2 (SQLite)
- ViewBinding
- Corrotinas (lifecycleScope)
- RecyclerView

> **Obs.:** o tutorial usa `id("kotlin-kapt")` e Room 2.6.1. Meu Android Studio cria projetos com
> AGP 9, que já tem o Kotlin embutido, e nele o `kotlin-kapt` não funciona. Por isso usei o plugin
> `com.android.legacy-kapt` (que faz o mesmo papel) e o Room 2.7.2, que é compatível com o Kotlin 2.x.

## Estrutura

```
app/src/main/
├── java/br/universidade/approomproduto/
│   ├── model/Product.kt        ← tabela (Entidade)
│   ├── dao/ProductDao.kt       ← comandos SQL (DAO)
│   ├── db/AppDatabase.kt       ← o banco de dados (Singleton)
│   └── view/
│       ├── MainActivity.kt     ← a tela
│       └── ProductAdapter.kt   ← adapter do RecyclerView
└── res/layout/
    ├── activity_main.xml
    └── item_product.xml
```

## Prints do app
![Tela inicial](docs/prints/tela-inicial.png)
![Logcat com a listagem](docs/prints/logcat.png)

## Parte A: Complemento prático

### A1. Limpar campos após inserir
**O que foi feito:** criei a função `limparCampos()`, chamada dentro da corrotina logo depois do
`insertAll()` dar certo. Ela apaga o texto de todos os campos de cadastro e coloca o cursor de volta
no campo Nome, para já digitar o próximo produto.

```kotlin
private fun limparCampos() {
    binding.editProductName.text.clear()
    binding.editProductPrice.text.clear()
    binding.editProductDescription.text.clear()
    binding.editProductQuantity.text.clear()
    binding.editProductName.requestFocus()
}
```
Print: ![A1](docs/prints/a1.png)

### A2. Validação de preço
**O que foi feito:** o preço é convertido com `toDoubleOrNull()`. Se o campo estiver vazio (ou com
texto inválido) o resultado é `null`. Se for `null`, zero ou negativo, o produto não é salvo e
a mensagem de erro aparece no próprio campo.

```kotlin
val price = binding.editProductPrice.text.toString().toDoubleOrNull()
...
if (price == null || price <= 0) {
    binding.editProductPrice.error = "Informe um preço válido"
    return
}
```
Print: ![A2](docs/prints/a2.png)

### A3. Lista na tela (com o desafio extra: RecyclerView)
**O que foi feito:** em vez de usar só o Logcat, os produtos aparecem numa `RecyclerView` abaixo dos
botões. Criei o layout `item_product.xml` (uma linha da lista) e o `ProductAdapter`, que
preenche cada linha com id, nome, preço, quantidade e descrição. Ao clicar em **Listar Produtos** a
lista é carregada do banco e enviada para o adapter. O Logcat continua sendo usado também.

```kotlin
// onCreate
adapter = ProductAdapter { product -> excluirProduto(product) }
binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
binding.recyclerProducts.adapter = adapter

private fun listarProdutos() {
    lifecycleScope.launch {
        val products = productDao.getAll()
        mostrarProdutos(products)
    }
}
```
Print: ![A3](docs/prints/a3.png)

### A4. Total do estoque
**O que foi feito:** junto da lista, um `TextView` mostra quantos produtos estão cadastrados
(`products.size`) e a soma dos preços (`products.sumOf { it.price }`). Ele é atualizado sempre que a
lista muda (inserir, listar, buscar, atualizar ou excluir).

```kotlin
private fun mostrarProdutos(products: List<Product>) {
    adapter.submitList(products)
    binding.textEmpty.text = if (products.isEmpty()) "Nenhum produto encontrado." else ""
    val total = products.sumOf { it.price }
    binding.textTotal.text = "Produtos: ${products.size} | Soma dos preços: R$ %.2f".format(total)
}
```
Print: ![A4](docs/prints/a4.png)

### A5. Buscar por nome
**O que foi feito:** criei no DAO a função `buscarPorNome` com `LIKE`, um campo de busca e o botão
**Buscar**. O texto digitado é colocado entre `%` para encontrar nomes que *contenham* o texto
(ex.: "tec" encontra "Teclado"). O resultado aparece na mesma lista.

```kotlin
// ProductDao.kt
@Query("SELECT * FROM product WHERE name LIKE :nome")
suspend fun buscarPorNome(nome: String): List<Product>

// MainActivity.kt
private fun buscarPorNome() {
    val texto = binding.editBusca.text.toString().trim()
    lifecycleScope.launch {
        val products = productDao.buscarPorNome("%$texto%")
        mostrarProdutos(products)
    }
}
```
Print: ![A5](docs/prints/a5.png)

### A6. Excluir produtos (com o desafio extra: excluir pelo id com @Delete)
**O que foi feito:** o botão **Excluir Todos** chama `deleteAll()`, que apaga a tabela inteira. Como
desafio extra, cada linha da lista tem um botão **Excluir** que remove só aquele produto usando
`@Delete` (o Room encontra a linha pela chave primária `id`).

```kotlin
// ProductDao.kt
@Query("DELETE FROM product")
suspend fun deleteAll()

@Delete
suspend fun delete(product: Product)

// MainActivity.kt
private fun excluirTodos() {
    lifecycleScope.launch {
        productDao.deleteAll()
        listarProdutos()
    }
}

private fun excluirProduto(product: Product) {
    lifecycleScope.launch {
        productDao.delete(product)
        listarProdutos()
    }
}
```
Print: ![A6](docs/prints/a6.png)

### A7. Atualizar produto
**O que foi feito:** adicionei dois campos (**Id** e **Novo preço**) e o botão **Atualizar**. O app
busca o produto pelo id; se ele existir, cria uma cópia com o novo preço (`copy`) e grava com
`@Update`. Se o id não existir ou o preço for inválido, mostra o erro no campo.

```kotlin
// ProductDao.kt
@Query("SELECT * FROM product WHERE id = :id")
suspend fun getById(id: Int): Product?

@Update
suspend fun update(product: Product)

// MainActivity.kt
lifecycleScope.launch {
    val product = productDao.getById(id)
    if (product == null) {
        binding.editUpdateId.error = "Produto não encontrado"
        return@launch
    }
    productDao.update(product.copy(price = novoPreco))
    listarProdutos()
}
```
Print: ![A7](docs/prints/a7.png)

### A8. Novo campo na tabela (`quantity`)
**O que foi feito:** adicionei `quantity: Int` na entidade `Product`, o campo **Quantidade em
estoque** no layout e subi a versão do banco para **2**.

```kotlin
// Product.kt
@ColumnInfo(name = "quantity") val quantity: Int = 0

// AppDatabase.kt
@Database(entities = [Product::class], version = 2)
...
.fallbackToDestructiveMigration(dropAllTables = true)
```

**O que aconteceu com os produtos já cadastrados:** eles **foram apagados**. Quando o app abriu
com a versão 2, o Room percebeu que o banco salvo no celular ainda era da versão 1 e procurou uma
*Migration* de 1 para 2. Como eu não escrevi nenhuma, o `fallbackToDestructiveMigration()` entrou em
ação: apagou as tabelas e criou tudo de novo já com a coluna `quantity`, então a tabela ficou vazia.
Sem o fallback, o app teria fechado com erro (`A migration from 1 to 2 was required but not found`).
Para não perder os dados, seria preciso criar uma migração, por exemplo:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0")
    }
}
// Room.databaseBuilder(...).addMigrations(MIGRATION_1_2)
```
Print: ![A8](docs/prints/a8.png)

## Parte B: Questões dissertativas

### B1. O que é persistência de dados? O que aconteceria com os produtos se fossem guardados apenas numa `MutableList` dentro da `MainActivity`?
**Resposta:** Persistência de dados é a capacidade de guardar informações de forma que elas
continuem existindo depois que o programa é fechado, ou seja, gravadas num armazenamento permanente
(no caso, o arquivo SQLite `database-name` dentro do celular) e não só na memória RAM. No meu projeto,
quando clico em Inserir, o `productDao.insertAll()` grava o produto nesse arquivo, e por isso eu fecho
o app, abro de novo, clico em Listar e os produtos continuam lá. Se eu usasse só uma `MutableList`
dentro da `MainActivity`, os produtos ficariam apenas na memória do objeto da Activity. Bastaria
fechar o app, o Android matar o processo para liberar memória, ou até girar a tela (que recria a
Activity) para a lista voltar vazia e tudo o que foi cadastrado ser perdido.

### B2. Qual a diferença entre usar o SQLite diretamente e usar o Room? Cite pelo menos duas vantagens do Room.
**Resposta:** Usando o SQLite diretamente, com `SQLiteOpenHelper`, eu teria que escrever à mão o
`CREATE TABLE`, os `INSERT`/`UPDATE`/`DELETE`, abrir e fechar o banco, e principalmente percorrer o
`Cursor` coluna por coluna para transformar cada linha num objeto `Product`. É muito código repetitivo
e fácil de errar. O Room é uma camada em cima do SQLite que gera esse código para mim a partir das
anotações. Vantagens: (1) as consultas SQL são **verificadas na compilação**, então se eu escrever
`SELECT * FROM produto` (tabela errada) o projeto nem compila, em vez de quebrar só quando o usuário
clicar; (2) muito **menos código**: no meu `ProductDao` um `@Insert` ou `@Update` já basta, e o Room
converte o resultado direto em `List<Product>`; (3) tem suporte nativo a **corrotinas** (`suspend`),
LiveData/Flow e a **migrações** de versão do banco.

### B3. Explique o papel de cada uma das três peças do Room: Entity, DAO e Database. Como elas se relacionam no seu projeto?
**Resposta:** A **Entity** (`@Entity`) é a classe que representa uma tabela: no meu projeto é a
`Product`, e cada atributo (`id`, `name`, `price`, `description`, `quantity`) virou uma coluna, com o
`id` como chave primária gerada automaticamente. O **DAO** (`@Dao`) é a interface com os comandos que
podem ser executados nessa tabela: o meu `ProductDao` tem `getAll`, `insertAll`, `buscarPorNome`,
`deleteAll`, `delete`, `getById` e `update`. O **Database** (`@Database`) é a classe que junta tudo:
o `AppDatabase` declara que o banco tem a entidade `Product`, está na versão 2 e fornece o
`productDao()`. A relação é: a `MainActivity` pede o banco ao `AppDatabase`, pega o `ProductDao` dele
e chama as funções do DAO passando ou recebendo objetos `Product`. Quem escreve a implementação real
do DAO e do banco é o Room, durante a compilação (via kapt).

### B4. Por que o Android não permite acessar o banco na thread principal? O que é um ANR? Como `suspend` e `lifecycleScope.launch` resolvem esse problema?
**Resposta:** A thread principal (UI thread) é a responsável por desenhar a tela e responder aos
toques do usuário. Acessar o banco envolve ler e gravar em disco, o que pode demorar, e enquanto isso
a thread principal fica parada: a tela congela e os botões não respondem. Se isso durar alguns
segundos, o Android mostra o **ANR (Application Not Responding)**, aquela janela dizendo que o app
não está respondendo e perguntando se o usuário quer fechar. Por isso o Room bloqueia por padrão e
lança `Cannot access database on the main thread`. No meu projeto, as funções do DAO são `suspend`,
e o Room as executa numa thread de segundo plano. Eu chamo essas funções dentro de
`lifecycleScope.launch { }`, que abre uma corrotina: ela "pausa" enquanto o banco trabalha, sem
travar a tela, e quando o resultado chega continua na thread principal, onde posso mostrar o `Toast`
e atualizar a lista. Além disso, o `lifecycleScope` cancela a corrotina sozinho se a Activity for
destruída.

### B5. O que faz o `fallbackToDestructiveMigration()`? Por que ele é perigoso num app publicado? O que seria uma migração?
**Resposta:** O `fallbackToDestructiveMigration()` diz ao Room que, se a versão do banco mudar e não
houver uma migração definida, ele pode **apagar todas as tabelas e recriá-las** com a estrutura
nova. Isso foi exatamente o que aconteceu no item A8: subi a versão para 2 e os produtos que eu já
tinha cadastrado sumiram. Durante o desenvolvimento isso é prático, mas num app publicado na Play
Store seria um desastre: numa atualização, todos os usuários perderiam seus dados (pedidos, cadastros,
anotações) sem aviso. Uma **migração** (`Migration`) é um código que diz como transformar o banco de
uma versão para a outra **mantendo os dados**, por exemplo um `ALTER TABLE product ADD COLUMN quantity
INTEGER NOT NULL DEFAULT 0` para ir da versão 1 para a 2. Ela é registrada com `addMigrations(...)` e o
Room a executa automaticamente quando o usuário atualiza o app.

### B6. Qual a vantagem do ViewBinding em relação ao `findViewById`? Que tipo de erro ele ajuda a evitar?
**Resposta:** Com o `findViewById` eu preciso buscar cada componente pelo id e informar o tipo, por
exemplo `findViewById<EditText>(R.id.editProductName)`, numa linha para cada campo. Se eu passar um
id que não existe naquele layout, ou fizer cast para o tipo errado, o código compila normalmente
e só quebra em tempo de execução, com `NullPointerException` ou `ClassCastException`. O ViewBinding
gera automaticamente uma classe para cada layout (`ActivityMainBinding`, `ItemProductBinding`) com
uma propriedade para cada view que tem id, já com o tipo certo. No meu projeto uso
`binding.editProductPrice` ou `binding.btnInserirProduto` direto. Isso dá **segurança contra nulos e
de tipo**: se eu renomear ou apagar um id no XML, o erro aparece na compilação, e o código fica bem
mais curto e legível.

### B7. Por que em apps maiores é recomendado criar o banco como Singleton? O que poderia acontecer se várias telas criassem cada uma o seu banco?
**Resposta:** Criar uma instância do `RoomDatabase` é uma operação cara: abre o arquivo, prepara
conexões e carrega o esquema. Se cada tela fizesse seu próprio `Room.databaseBuilder(...)`, o app
gastaria memória e tempo à toa e teria várias conexões abertas para o mesmo arquivo. Isso pode
causar problemas de concorrência: uma tela grava enquanto outra lê por outra instância, gerando
dados desatualizados, erros de `database is locked` ou até corrupção. Com Singleton existe **uma única
instância para o app inteiro**, compartilhada por todas as telas. Por isso, no meu projeto, mudei a
criação do banco para um `companion object` no `AppDatabase` com `getInstance(context)`, usando
`@Volatile` e `synchronized` para garantir que, mesmo com duas threads chamando ao mesmo tempo, o banco
seja criado uma vez só. A `MainActivity` só chama `AppDatabase.getInstance(this).productDao()`.

### B8. Reflexão pessoal: principais dificuldades e um app real em que o Room seria útil
**Resposta:** A maior dificuldade foi fazer o projeto compilar seguindo o tutorial ao pé da letra. O
Android Studio que eu uso gera projetos com o AGP 9, que já tem o Kotlin embutido, então o
`id("kotlin-kapt")` e o `alias(libs.plugins.kotlin.android)` do tutorial não funcionavam. Resolvi
pesquisando e trocando pelo plugin `com.android.legacy-kapt`, e atualizei o Room para a 2.7.2 porque a
2.6.1 não lê bem o Kotlin 2.x. Outra dificuldade foi entender por que os produtos sumiram ao adicionar
o campo `quantity`, o que me fez entender na prática o que é versão do banco e migração. Também
precisei me acostumar com corrotinas para atualizar a tela depois de cada operação no banco. Um app
real em que o Room seria muito útil é um **app de controle de estoque para uma pequena loja ou
mercadinho**: o funcionário cadastraria produtos com preço e quantidade, daria baixa nas vendas e
consultaria pelo nome, tudo funcionando **offline** dentro do celular, mesmo sem internet. Eu aplicaria
exatamente a estrutura deste projeto (`Product` com `quantity`, um DAO com busca, update e delete),
adicionando novas entidades como `Venda` e migrações de verdade em vez do `fallbackToDestructiveMigration`.

## Como executar
1. Clone o repositório
2. Abra no Android Studio
3. Execute em um emulador (API 24+)
