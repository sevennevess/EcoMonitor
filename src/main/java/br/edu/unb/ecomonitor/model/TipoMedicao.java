package br.edu.unb.ecomonitor.model;

public enum TipoMedicao {
    TEMPERATURA("C", -50, 100),
    UMIDADE("%", 0, 100),
    QUALIDADE_AR("IQA", 0, 500);

    private final String unidade;
    private final double minimo;
    private final double maximo;

    TipoMedicao(String unidade, double minimo, double maximo) {
        this.unidade = unidade;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String getUnidade() { return unidade; }
    public double getMinimo() { return minimo; }
    public double getMaximo() { return maximo; }
}
