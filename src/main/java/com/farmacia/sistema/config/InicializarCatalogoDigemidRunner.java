package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.digemid.CatalogoDigemid;
import com.farmacia.sistema.domain.digemid.CatalogoDigemidRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class InicializarCatalogoDigemidRunner implements ApplicationRunner {

    private final CatalogoDigemidRepository repo;

    public InicializarCatalogoDigemidRunner(CatalogoDigemidRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;

        // ═══════════════════════════════════════════════════════════
        // LISTA I - ESTUPEFACIENTES (Receta Especial Numerada)
        // Máximo control. Opioides potentes y derivados.
        // ═══════════════════════════════════════════════════════════
        est1("Morfina", "Morfina Clorhidrato");
        est1("Fentanilo", "Durogesic,Fentanest");
        est1("Metadona", "Metasedin");
        est1("Petidina", "Meperidina,Demerol");
        est1("Oxicodona", "OxyContin,Oxycodone");
        est1("Hidrocodona", null);
        est1("Hidromorfona", "Dilaudid");
        est1("Heroina", null);
        est1("Opio", null);
        est1("Sufentanilo", "Sufenta");
        est1("Alfentanilo", "Alfenta");
        est1("Remifentanilo", "Ultiva");
        est1("Acetilmetadol", null);
        est1("Alfaprodina", null);
        est1("Bezitramida", null);
        est1("Cetobemidona", null);
        est1("Dextromoramida", null);
        est1("Difenoxilato", null);
        est1("Dipipanona", null);
        est1("Etorfina", null);
        est1("Fenadona", null);
        est1("Fenazocina", null);
        est1("Levorfanol", null);
        est1("Normetadona", null);
        est1("Piritramida", null);
        est1("Tilidina", null);
        est1("Trimeperidina", null);
        est1("Coca (hojas para extraccion)", null);
        est1("Cocaina", null);
        est1("Ecgonina", null);
        est1("Codeina (concentracion alta)", null);

        // ═══════════════════════════════════════════════════════════
        // LISTA II - ESTUPEFACIENTES menor control (Receta Especial)
        // ═══════════════════════════════════════════════════════════
        est2("Codeina", "Codeina Fosfato");
        est2("Dihidrocodeina", null);
        est2("Dextropropoxifeno", null);
        est2("Tramadol", "Tramal,Tramadol");
        est2("Tapentadol", "Nucynta");
        est2("Buprenorfina", "Subutex,Temgesic");
        est2("Nalbufina", "Nubain");
        est2("Pentazocina", "Talwin");

        // ═══════════════════════════════════════════════════════════
        // LISTA III - PSICOTRÓPICOS (Receta Especial)
        // Barbitúricos y estimulantes anfetamínicos
        // ═══════════════════════════════════════════════════════════
        psi3("Anfetamina", null);
        psi3("Metilfenidato", "Ritalin,Concerta");
        psi3("Lisdexanfetamina", "Vyvanse");
        psi3("Fenmetrazina", null);
        psi3("Secobarbital", null);
        psi3("Pentobarbital", "Nembutal");
        psi3("Amobarbital", null);
        psi3("Ciclobarbital", null);
        psi3("Glutetimida", null);
        psi3("Catina", null);
        psi3("Fenciclidina", null);
        psi3("Mecloqualone", null);
        psi3("Metacualona", null);
        psi3("Zipeprol", null);
        psi3("Ketamina", "Ketalar");
        psi3("GHB (Acido gamma-hidroxibutirico)", "Xyrem");

        // ═══════════════════════════════════════════════════════════
        // LISTA IV - PSICOTRÓPICOS (Receta Retenida)
        // Benzodiazepinas, barbitúricos menores, hipnóticos
        // ═══════════════════════════════════════════════════════════
        psi4("Diazepam", "Valium,Diazepam");
        psi4("Clonazepam", "Rivotril,Klonopin");
        psi4("Alprazolam", "Xanax,Alprazolam");
        psi4("Lorazepam", "Ativan,Lorazepam");
        psi4("Bromazepam", "Lexotan,Lexotanil");
        psi4("Midazolam", "Dormicum,Versed");
        psi4("Flunitrazepam", "Rohypnol");
        psi4("Nitrazepam", "Mogadon");
        psi4("Triazolam", "Halcion");
        psi4("Clobazam", "Frisium,Onfi");
        psi4("Clorazepato", "Tranxene");
        psi4("Estazolam", "ProSom");
        psi4("Flurazepam", "Dalmane");
        psi4("Halazepam", null);
        psi4("Ketazolam", null);
        psi4("Lormetazepam", "Noctamid");
        psi4("Medazepam", "Nobrium");
        psi4("Oxazepam", "Serax");
        psi4("Pinazepam", null);
        psi4("Prazepam", null);
        psi4("Temazepam", "Restoril");
        psi4("Tetrazepam", null);
        psi4("Clotiazepam", null);
        psi4("Nordazepam", null);
        psi4("Nimetazepam", null);
        psi4("Fenobarbital", "Luminal,Fenobarbital");
        psi4("Barbital", "Veronal");
        psi4("Alobarbital", null);
        psi4("Butalbital", null);
        psi4("Vinilbital", null);
        psi4("Zolpidem", "Stilnox,Ambien");
        psi4("Zopiclona", "Imovane");
        psi4("Zaleplon", "Sonata");
        psi4("Eszopiclona", "Lunesta");
        psi4("Modafinilo", "Provigil,Modiodal");
        psi4("Armodafinilo", "Nuvigil");
        psi4("Pregabalina", "Lyrica,Pregabalina");
        psi4("Gabapentina", "Neurontin,Gabapentina");
        psi4("Carisoprodol", "Soma");
        psi4("Meprobamato", null);
        psi4("Pemolina", null);
        psi4("Mazindol", null);
        psi4("Fentermina", null);
        psi4("Propilhexedrina", null);
        psi4("Aminorex", null);
        psi4("Clordiazepoxido", "Librium");
        psi4("Cloxazolam", null);
        psi4("Delorazepam", null);
        psi4("Difebarbamat", null);
        psi4("Etinamato", null);
        psi4("Lefetamina", null);
        psi4("Loprazolam", null);

        // ═══════════════════════════════════════════════════════════
        // SUJETOS A FISCALIZACIÓN (Receta Retenida)
        // Precursores y sustancias bajo vigilancia
        // ═══════════════════════════════════════════════════════════
        suj("Pseudoefedrina", "Sudafed");
        suj("Efedrina", null);
        suj("Ergotamina", "Cafergot,Migranox");
        suj("Ergometrina", null);
        suj("Acido lisergico", null);
        suj("Piperonal", null);
        suj("Safrol", null);
        suj("Anhidrido acetico", null);
        suj("Acetona (uso farmaceutico)", null);
        suj("Acido antranilico", null);
        suj("Acido fenilacetico", null);
        suj("Isosafrol", null);
        suj("Permanganato de potasio (uso farmac.)", null);
        suj("Tolueno (uso farmaceutico)", null);
        suj("Testosterona", "Sostenon,Testex");
        suj("Nandrolona", "Deca-Durabolin");
        suj("Estanozolol", "Winstrol");
        suj("Oximetolona", "Anadrol");
        suj("Oxandrolona", "Anavar");
        suj("Boldenona", null);
        suj("Trembolona", null);
        suj("Metenolona", "Primobolan");
        suj("Danazol", "Ladogal");
        suj("Mesterolona", "Proviron");
        suj("Clostebol", null);
        suj("Drostanolona", null);
        suj("Fluoximesterona", "Halotestin");
        suj("Metandienona", "Dianabol");
        suj("Metiltestosterona", null);

        // ═══════════════════════════════════════════════════════════
        // MEDICAMENTOS QUE REQUIEREN RECETA PERO NO SON CONTROLADOS
        // (Receta Simple - antibióticos, etc.)
        // ═══════════════════════════════════════════════════════════
        recetaSimple("Amoxicilina", "Amoxil");
        recetaSimple("Azitromicina", "Zithromax,Azitromicina");
        recetaSimple("Ciprofloxacino", "Ciprofloxacina");
        recetaSimple("Levofloxacino", "Tavanic");
        recetaSimple("Metronidazol", "Flagyl");
        recetaSimple("Cefalexina", "Keflex");
        recetaSimple("Ceftriaxona", "Rocephin");
        recetaSimple("Clindamicina", "Dalacin");
        recetaSimple("Doxiciclina", "Vibramicina");
        recetaSimple("Claritromicina", "Klaricid");
        recetaSimple("Sulfametoxazol + Trimetoprima", "Bactrim");
        recetaSimple("Gentamicina", null);
        recetaSimple("Amikacina", null);
        recetaSimple("Vancomicina", null);
        recetaSimple("Meropenem", null);
        recetaSimple("Imipenem", null);
        recetaSimple("Fluconazol", "Diflucan");
        recetaSimple("Itraconazol", "Sporanox");
        recetaSimple("Ketoconazol", "Nizoral");
        recetaSimple("Aciclovir", "Zovirax");
        recetaSimple("Oseltamivir", "Tamiflu");
        recetaSimple("Warfarina", "Coumadin");
        recetaSimple("Insulina", "Lantus,Humalog,NovoRapid");
        recetaSimple("Metformina", "Glucophage");
        recetaSimple("Glibenclamida", null);
        recetaSimple("Atorvastatina", "Lipitor");
        recetaSimple("Rosuvastatina", "Crestor");
        recetaSimple("Enalapril", "Renitec");
        recetaSimple("Losartan", "Cozaar");
        recetaSimple("Amlodipino", "Norvasc");
        recetaSimple("Metoprolol", "Lopressor");
        recetaSimple("Atenolol", "Tenormin");
        recetaSimple("Furosemida", "Lasix");
        recetaSimple("Hidroclorotiazida", null);
        recetaSimple("Espironolactona", "Aldactone");
        recetaSimple("Omeprazol", "Losec");
        recetaSimple("Lansoprazol", "Prevacid");
        recetaSimple("Esomeprazol", "Nexium");
        recetaSimple("Prednisona", null);
        recetaSimple("Dexametasona", null);
        recetaSimple("Metilprednisolona", "Medrol");
        recetaSimple("Levotiroxina", "Eutirox,Synthroid");
        recetaSimple("Carbamazepina", "Tegretol");
        recetaSimple("Acido Valproico", "Depakene,Valproato");
        recetaSimple("Lamotrigina", "Lamictal");
        recetaSimple("Levetiracetam", "Keppra");
        recetaSimple("Topiramato", "Topamax");
        recetaSimple("Fenitoina", "Epamin,Dilantin");
        recetaSimple("Fluoxetina", "Prozac");
        recetaSimple("Sertralina", "Zoloft");
        recetaSimple("Paroxetina", "Paxil");
        recetaSimple("Escitalopram", "Lexapro");
        recetaSimple("Venlafaxina", "Effexor");
        recetaSimple("Duloxetina", "Cymbalta");
        recetaSimple("Amitriptilina", "Tryptanol");
        recetaSimple("Haloperidol", "Haldol");
        recetaSimple("Risperidona", "Risperdal");
        recetaSimple("Olanzapina", "Zyprexa");
        recetaSimple("Quetiapina", "Seroquel");
        recetaSimple("Aripiprazol", "Abilify");
        recetaSimple("Litio Carbonato", "Eskalith");
        recetaSimple("Metotrexato", null);
        recetaSimple("Ciclosporina", "Sandimmun");
        recetaSimple("Misoprostol", "Cytotec");
        recetaSimple("Sildenafilo", "Viagra");
        recetaSimple("Tadalafilo", "Cialis");
        recetaSimple("Isotretinoina", "Roaccutan");
        recetaSimple("Clomifeno", "Clomid");
        recetaSimple("Tamoxifeno", "Nolvadex");
        recetaSimple("Finasterida", "Proscar");
        recetaSimple("Minoxidil oral", null);
    }

    private void est1(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "ESTUPEFACIENTE", "LISTA_I", "RECETA_ESPECIAL_NUMERADA", true,
                "Estupefaciente Lista I - Control máximo");
    }

    private void est2(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "ESTUPEFACIENTE", "LISTA_II", "RECETA_ESPECIAL", true,
                "Estupefaciente Lista II");
    }

    private void psi3(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "PSICOTROPICO", "LISTA_III", "RECETA_ESPECIAL", true,
                "Psicotrópico Lista III");
    }

    private void psi4(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "PSICOTROPICO", "LISTA_IV", "RECETA_RETENIDA", true,
                "Psicotrópico Lista IV");
    }

    private void suj(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "SUJETO_FISCALIZACION", "LISTA_IV", "RECETA_RETENIDA", true,
                "Sujeto a fiscalización");
    }

    private void recetaSimple(String principioActivo, String comercial) {
        guardar(principioActivo, comercial, "", "", "RECETA_SIMPLE", false,
                "Requiere receta médica simple");
    }

    private void guardar(String principioActivo, String comercial, String tipo, String lista,
                         String tipoReceta, boolean controlEspecial, String obs) {
        if (repo.existsByPrincipioActivo(principioActivo)) return;
        CatalogoDigemid c = new CatalogoDigemid();
        c.setPrincipioActivo(principioActivo);
        c.setNombreComercial(comercial);
        c.setTipoProductoControlado(tipo != null && !tipo.isEmpty() ? tipo : null);
        c.setListaControl(lista != null && !lista.isEmpty() ? lista : null);
        c.setRequiereReceta(true);
        c.setTipoReceta(tipoReceta);
        c.setControlStockEspecial(controlEspecial);
        c.setObservacion(obs);
        repo.save(c);
    }
}
