// Roda no processo principal do Jest, antes dos workers: é o único ponto em que
// mudar o TZ vale para os testes. O runner do CI é UTC e o usuário é UTC-3.
module.exports = () => {
  process.env.TZ = 'America/Sao_Paulo';
};
