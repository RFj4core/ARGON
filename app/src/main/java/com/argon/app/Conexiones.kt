package com.argon.app

class Conexion(val id: String, val nombre: String, val herramientas: List<String>)

object Conexiones {

    val todas = listOf(
        Conexion("telefono", "Teléfono: batería y ubicación", listOf("ver_bateria", "ver_ubicacion")),
        Conexion("contactos", "Contactos y llamadas", listOf("leer_contactos", "llamar")),
        Conexion("sms", "Mensajes SMS (lectura)", listOf("leer_sms")),
        Conexion("reloj", "Reloj: alarmas y temporizadores", listOf("poner_alarma", "poner_temporizador")),
        Conexion("archivos", "Guardar archivos y código", listOf("guardar_archivo")),
        Conexion("internet", "Internet: buscar y leer páginas", listOf("buscar_web", "abrir_pagina")),
        Conexion("imagenes", "Generar imágenes (Pollinations)", listOf("generar_imagen")),
        Conexion("apps", "Abrir apps y enlaces, compartir", listOf("abrir_app", "abrir_enlace", "compartir_texto"))
    )

    val sensibles = setOf(
        "leer_contactos", "llamar", "leer_sms", "ver_ubicacion",
        "guardar_archivo", "abrir_app", "abrir_enlace", "compartir_texto"
    )
}
