package com.argon.app

class Conexion(val id: String, val nombre: String, val herramientas: List<String>)

object Conexiones {

    val todas = listOf(
        Conexion(
            "telefono", "Teléfono: batería, ubicación, linterna, volumen, ajustes",
            listOf("ver_bateria", "ver_ubicacion", "info_telefono", "linterna", "volumen", "abrir_ajustes")
        ),
        Conexion("contactos", "Contactos y llamadas", listOf("leer_contactos", "llamar", "crear_contacto")),
        Conexion("sms", "Mensajes SMS (lectura)", listOf("leer_sms")),
        Conexion(
            "mensajes", "Redactar SMS, correo y WhatsApp (tú envías)",
            listOf("escribir_sms", "escribir_correo", "escribir_whatsapp")
        ),
        Conexion("reloj", "Reloj: alarmas y temporizadores", listOf("poner_alarma", "poner_temporizador")),
        Conexion("calendario", "Calendario: ver y crear eventos", listOf("ver_agenda", "crear_evento")),
        Conexion(
            "notas", "Notas y rutinas básicas",
            listOf("guardar_nota", "ver_notas", "borrar_nota", "guardar_rutina", "ver_rutinas", "borrar_rutina")
        ),
        Conexion(
            "rutinas", "Rutinas avanzadas: variables, editar y programar",
            listOf(
                "rutina_agregar_paso", "rutina_quitar_paso", "rutina_renombrar", "rutina_ejecutar",
                "rutina_programar", "rutina_programaciones", "rutina_quitar_programacion"
            )
        ),
        Conexion(
            "archivos", "Archivos y código (carpeta Descargas/Argon)",
            listOf("guardar_archivo", "listar_archivos", "leer_archivo", "borrar_archivo")
        ),
        Conexion(
            "proyectos", "Proyectos de código con respaldo y vista previa",
            listOf(
                "proyecto_listar", "proyecto_leer", "proyecto_guardar", "proyecto_borrar",
                "proyecto_versiones", "proyecto_restaurar", "proyecto_zip", "vista_previa"
            )
        ),
        Conexion("portapapeles", "Portapapeles", listOf("leer_portapapeles", "copiar_portapapeles")),
        Conexion("internet", "Internet: buscar, leer páginas, Wikipedia", listOf("buscar_web", "abrir_pagina", "wikipedia")),
        Conexion("datos", "Clima, noticias, divisas y calculadora", listOf("clima", "noticias", "tipo_cambio", "calcular")),
        Conexion("imagenes", "Generar imágenes (Pollinations)", listOf("generar_imagen")),
        Conexion("musica", "Música y medios", listOf("musica", "reproducir")),
        Conexion("mapas", "Mapas y navegación", listOf("abrir_mapa")),
        Conexion(
            "apps", "Abrir apps y enlaces, compartir",
            listOf("abrir_app", "listar_apps", "abrir_enlace", "compartir_texto")
        ),
        Conexion(
            "integraciones", "Integraciones: YouTube, Spotify, Telegram, tienda, cámara, galería...",
            listOf(
                "youtube_buscar", "spotify_buscar", "telegram_enviar", "play_store_buscar",
                "buscar_en_navegador", "abrir_camara", "abrir_galeria", "abrir_calculadora",
                "ajustes_de_app", "compartir_en_app"
            )
        )
    )

    val sensibles = setOf(
        "leer_contactos", "llamar", "crear_contacto", "leer_sms", "ver_ubicacion",
        "guardar_archivo", "leer_archivo", "borrar_archivo", "listar_archivos",
        "ver_agenda", "leer_portapapeles", "borrar_nota", "borrar_rutina",
        "proyecto_borrar", "proyecto_restaurar",
        "rutina_programar", "rutina_quitar_programacion",
        "telegram_enviar", "compartir_en_app",
        "abrir_app", "abrir_enlace", "compartir_texto"
    )
}
