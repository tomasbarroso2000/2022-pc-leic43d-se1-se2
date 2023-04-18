# PC - Verão 22/23 - Série de exercícios

Repositório com a resolução das séries do aluno 48333.
Por favor, crie também o ficheiro `ids.md` contendo apenas o seu número de aluno.

## Observações

A implementação do método "fun <T> execute(callable: Callable<T>): Future<T>" precisa de ajustes
em relação ao tratamento de exceções do Future neste caso se existir algum problema e for necessário atualizar o Future com esse erro
"chamada onError", não acontece nada.

## Documentação técnica

Tanto quanto possivel, os exercicios da série foram implementados com a técnica
delegação de execução/kernel-style e com Specific Notification para
tornar mais eficiente a sinalização de uma thread especifica, que se encontra bloqueada em vez de
sinalizar todas as threads existentes
